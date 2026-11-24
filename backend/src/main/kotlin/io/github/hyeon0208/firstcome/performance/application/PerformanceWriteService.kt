package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.ParticipantNo
import io.github.hyeon0208.firstcome.performance.PerformanceCreated
import io.github.hyeon0208.firstcome.performance.PerformanceEnded
import io.github.hyeon0208.firstcome.performance.PerformancePurged
import io.github.hyeon0208.firstcome.performance.PerformanceResult
import io.github.hyeon0208.firstcome.performance.application.dto.CreatePerformanceRequest
import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceCheckResponse
import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceResponse
import io.github.hyeon0208.firstcome.performance.domain.ParticipantEntity
import io.github.hyeon0208.firstcome.performance.domain.PerformanceEntity
import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatStatus
import io.github.hyeon0208.firstcome.performance.domain.PerformanceStatus
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaParticipantRepository
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceRepository
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceSeatRepository
import io.github.hyeon0208.firstcome.performance.infrastructure.PerformanceSeatJdbcRepository
import io.github.hyeon0208.firstcome.show.application.ShowReadService
import io.github.hyeon0208.firstcome.support.error.requireRule
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 회차의 생애: 만들기 → 입장 → 끝(성공, 매진, 시간 초과, 중단) → 지우기.
@Service
@Transactional
class PerformanceWriteService(
    private val performanceRepository: JpaPerformanceRepository,
    private val participantRepository: JpaParticipantRepository,
    private val performanceSeatRepository: JpaPerformanceSeatRepository,
    private val performanceSeatJdbcRepository: PerformanceSeatJdbcRepository,
    private val showReadService: ShowReadService,
    private val eventPublisher: ApplicationEventPublisher,
    private val bookingProperties: BookingProperties,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // 그 사용자만의 회차를 만든다. 좌석 한 벌을 새로 만들고, 이전에 끝난 회차의 데이터는 지운다.
    fun createPerformance(userId: Long, request: CreatePerformanceRequest): PerformanceResponse {
        val seats = showReadService.getSeats(request.showId)
        val now = LocalDateTime.now(clock)
        endIfDue(performanceRepository.findByActiveUserIdForUpdate(userId), now)
        requireRule(!performanceRepository.existsByActiveUserId(userId), PerformanceErrorCode.ALREADY_IN_PROGRESS)
        requireRule(
            performanceRepository.countByStatus(PerformanceStatus.ACTIVE) < bookingProperties.maxActivePerformances,
            PerformanceErrorCode.PRACTICE_FULL,
        )
        performanceRepository.findEndedIdsByUserId(userId).forEach(::purge)

        val performance = performanceRepository.save(
            PerformanceEntity(
                userId = userId,
                showId = request.showId,
                difficulty = request.difficulty,
                seed = Random.nextLong(),
                openAt = now.plus(bookingProperties.openDelay),
                startAt = now.toLocalDate().plusDays(bookingProperties.showStartDays).atTime(SHOW_START_TIME),
            ),
        )
        participantRepository.save(ParticipantEntity(performanceId = performance.id, participantNo = ParticipantNo.USER))
        performanceSeatJdbcRepository.insertAvailableSeats(performance.id, seats.map { it.seatId })
        eventPublisher.publishEvent(
            PerformanceCreated(performance.id, performance.showId, performance.difficulty, performance.seed, performance.openAt),
        )
        logger.info(
            "회차 생성 performanceId={} userId={} showId={} difficulty={} openAt={}",
            performance.id,
            userId,
            performance.showId,
            performance.difficulty,
            performance.openAt,
        )
        return PerformanceResponse.of(performance, now)
    }

    // 사용자의 진행 중 회차. 제한 시간이 지났으면 끝내고 없는 것으로 답한다.
    fun findCurrentPerformance(userId: Long): PerformanceResponse? {
        val now = LocalDateTime.now(clock)
        val performance = performanceRepository.findByActiveUserIdForUpdate(userId)
        endIfDue(performance, now)
        return performance?.takeIf { it.isActive }?.let { PerformanceResponse.of(it, now) }
    }

    // 사용자의 요청이 닿은 회차가 진행 중인지 확인한다. 제한 시간이 지났으면 여기서 끝낸다.
    // 끝낸 것이 커밋되도록 예외를 던지지 않고 상태를 돌려준다(BookingFacade가 오류로 바꾼다).
    fun checkIn(userId: Long, performanceId: Long): PerformanceCheckResponse {
        val now = LocalDateTime.now(clock)
        val performance = performanceRepository.findByIdForUpdate(performanceId)?.takeIf { it.userId == userId }
            ?: return PerformanceCheckResponse.GONE
        endIfDue(performance, now)
        return when {
            performance.isActive -> PerformanceCheckResponse.active(PerformanceResponse.of(performance, now))
            performance.result == PerformanceResult.TIMEOUT -> PerformanceCheckResponse.TIMED_OUT
            else -> PerformanceCheckResponse.GONE
        }
    }

    fun enter(performanceId: Long): PerformanceResponse {
        val now = LocalDateTime.now(clock)
        val performance = checkNotNull(performanceRepository.findByIdForUpdate(performanceId)) { "회차가 없습니다. performanceId=$performanceId" }
        performance.enter(now, bookingProperties.seatSelectionDuration)
        return PerformanceResponse.of(performance, now)
    }

    // 진행 중인 회차를 중단으로 끝낸다. 진행 중인 회차가 없으면 아무 일도 하지 않는다.
    fun abandon(userId: Long) {
        val performance = performanceRepository.findByActiveUserIdForUpdate(userId) ?: return
        requireRule(!isPaying(performance.id), PerformanceErrorCode.SEATS_PAYING)
        end(performance, PerformanceResult.ABANDONED, LocalDateTime.now(clock))
    }

    fun end(performanceId: Long, result: PerformanceResult) {
        val performance = performanceRepository.findByIdForUpdate(performanceId) ?: return
        end(performance, result, LocalDateTime.now(clock))
    }

    // 요청이 끊긴 회차(브라우저를 닫은 경우)를 정리 작업이 끝낸다.
    fun endIfDue(performanceId: Long) {
        endIfDue(performanceRepository.findByIdForUpdate(performanceId), LocalDateTime.now(clock))
    }

    // 서버가 다시 뜨면 진행 중이던 회차를 모두 중단으로 끝낸다. 가상 경쟁자의 일정과 진행 중이던 결제가 메모리에 있었기 때문이다.
    fun endAllActive(): Int {
        val now = LocalDateTime.now(clock)
        val ids = performanceRepository.findIdsByStatus(PerformanceStatus.ACTIVE)
        ids.forEach { id -> performanceRepository.findByIdForUpdate(id)?.let { end(it, PerformanceResult.ABANDONED, now) } }
        return ids.size
    }

    // 끝난 회차의 좌석과 참가자, 그리고 그 회차에 딸린 다른 모듈의 데이터(예매)를 지운다. 연습 기록은 남는다.
    fun purge(performanceId: Long) {
        val performance = performanceRepository.findByIdForUpdate(performanceId) ?: return
        check(!performance.isActive) { "진행 중인 회차는 지울 수 없습니다. performanceId=$performanceId" }
        // 좌석을 바꾸는 요청과 같은 순서(참가자 → 좌석)로 잠가, 늦게 도착한 요청과 교착하지 않게 한다.
        participantRepository.deleteAllByPerformanceId(performanceId)
        performanceSeatRepository.deleteAllByPerformanceId(performanceId)
        eventPublisher.publishEvent(PerformancePurged(performanceId))
        performanceRepository.deleteById(performanceId)
        logger.info("회차 데이터 삭제 performanceId={}", performanceId)
    }

    // 제한 시간이 지났는지 판정한다.
    // - 입장 전: 오픈 뒤 일정 시간 안에 들어오지 않으면 중단
    // - 선점 전: 좌석 선택 시간이 끝나면 시간 초과
    // - 선점 뒤: 예매 가능 시간이 끝나면 시간 초과. 좌석 선택 시간이 끝나도 선점 중이면 예매 가능 시간을 끝까지 준다.
    //   결제 중이면 확정 단계가 결과를 정하도록 기다리되, 결제 결과가 끝내 오지 않으면 유예 시간 뒤에 끝낸다.
    private fun endIfDue(performance: PerformanceEntity?, now: LocalDateTime) {
        if (performance == null || !performance.isActive) {
            return
        }
        if (!performance.isEntered) {
            if (performance.isEnterGraceOverAt(now, bookingProperties.enterGrace)) end(performance, PerformanceResult.ABANDONED, now)
            return
        }
        val holdExpiresAt = participantRepository.findByPerformanceIdAndParticipantNo(performance.id, ParticipantNo.USER)?.holdExpiresAt
        val isOver = when {
            holdExpiresAt == null -> performance.isSeatSelectionOverAt(now)
            isPaying(performance.id) -> holdExpiresAt.plus(bookingProperties.payingGrace).isBefore(now)
            else -> holdExpiresAt.isBefore(now)
        }
        if (isOver) {
            end(performance, PerformanceResult.TIMEOUT, now)
        }
    }

    private fun isPaying(performanceId: Long): Boolean =
        performanceSeatRepository.existsByPerformanceIdAndParticipantNoAndStatus(performanceId, ParticipantNo.USER, PerformanceSeatStatus.PAYING)

    private fun end(performance: PerformanceEntity, result: PerformanceResult, now: LocalDateTime) {
        if (!performance.isActive) {
            return
        }
        performance.end(result, now)
        eventPublisher.publishEvent(
            PerformanceEnded(
                performanceId = performance.id,
                userId = performance.userId,
                showId = performance.showId,
                difficulty = performance.difficulty,
                result = result,
                elapsedSeconds = performance.elapsedSeconds(),
                endedAt = now,
            ),
        )
        logger.info("회차 종료 performanceId={} userId={} result={} elapsedSeconds={}", performance.id, performance.userId, result, performance.elapsedSeconds())
    }

    companion object {
        private val SHOW_START_TIME: LocalTime = LocalTime.of(18, 0)
    }
}
