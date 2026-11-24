package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.application.dto.HoldSeatsResponse
import io.github.hyeon0208.firstcome.performance.application.dto.PreselectResult
import io.github.hyeon0208.firstcome.performance.domain.ParticipantEntity
import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatStatus
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaParticipantRepository
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceRepository
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceSeatRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.error.requireRule
import java.time.Clock
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 한 참가자가 좌석을 가져가는 경로. 사용자와 가상 경쟁자가 똑같이 이 서비스를 쓴다.
// 모든 메서드는 참가자 행을 먼저 잠가, 한 참가자의 요청을 한 번에 하나씩 처리한다.
@Service
@Transactional
class PerformanceSeatWriteService(
    private val performanceRepository: JpaPerformanceRepository,
    private val participantRepository: JpaParticipantRepository,
    private val performanceSeatRepository: JpaPerformanceSeatRepository,
    private val bookingProperties: BookingProperties,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun join(performanceId: Long, participantNo: Int) {
        participantRepository.save(ParticipantEntity(performanceId = performanceId, participantNo = participantNo))
    }

    fun lockParticipant(performanceId: Long, participantNo: Int) {
        getParticipantForUpdate(performanceId, participantNo)
    }

    // 가선점: 좌석을 누르는 순간 그 좌석을 묶는다.
    fun preselect(performanceId: Long, participantNo: Int, seatId: Long): PreselectResult {
        val participant = getParticipantForUpdate(performanceId, participantNo)
        val now = LocalDateTime.now(clock)
        val mySeats = performanceSeatRepository.findAllByPerformanceIdAndParticipantNo(performanceId, participantNo)
        requireRule(mySeats.none { it.status in STATUSES_AFTER_HOLD }, PerformanceErrorCode.ALREADY_HOLDING)
        val selectedSeatIds = mySeats.filter { it.isTakenAt(now) }.map { it.seatId }
        if (seatId in selectedSeatIds) {
            return PreselectResult.SELECTED
        }
        requireRule(selectedSeatIds.size < bookingProperties.maxSeats, PerformanceErrorCode.SEAT_LIMIT_EXCEEDED)

        // 가선점 시간이 남아 있으면 그 시각을 그대로 쓰고, 없거나 끝났으면 이 좌석부터 새로 센다.
        val startsNewWindow = participant.alivePreselectExpiresAt(now) == null
        val expiresAt = participant.alivePreselectExpiresAt(now) ?: now.plus(bookingProperties.preselectDuration)
        if (startsNewWindow) {
            performanceSeatRepository.releaseLapsedPreselects(performanceId, participantNo, now)
        }
        if (performanceSeatRepository.preselect(performanceId, seatId, participantNo, expiresAt, now) == 0) {
            requireRule(performanceSeatRepository.existsByPerformanceIdAndSeatId(performanceId, seatId), PerformanceErrorCode.SEAT_NOT_FOUND)
            return if (performanceSeatRepository.countAvailable(performanceId, now) == 0L) PreselectResult.SOLD_OUT else PreselectResult.TAKEN
        }
        if (startsNewWindow) {
            // 좌석을 실제로 가져간 뒤에만 시계를 시작한다. 좌석 UPDATE가 영속성 컨텍스트를 비웠으므로 참가자를 다시 읽는다.
            getParticipantForUpdate(performanceId, participantNo).startPreselect(now, bookingProperties.preselectDuration)
        }
        return PreselectResult.SELECTED
    }

    fun unpreselect(performanceId: Long, participantNo: Int, seatId: Long) {
        getParticipantForUpdate(performanceId, participantNo)
        performanceSeatRepository.unpreselect(performanceId, participantNo, seatId, LocalDateTime.now(clock))
    }

    fun unpreselectAll(performanceId: Long, participantNo: Int) {
        getParticipantForUpdate(performanceId, participantNo)
        performanceSeatRepository.unpreselectAll(performanceId, participantNo, LocalDateTime.now(clock))
    }

    // 선점: 가선점한 좌석 전체를 한 번에 묶는다. 가선점 시간이 끝나 풀린 좌석이 있으면 아무것도 선점하지 않는다.
    fun hold(performanceId: Long, participantNo: Int): HoldSeatsResponse {
        val participant = getParticipantForUpdate(performanceId, participantNo)
        val now = LocalDateTime.now(clock)
        val preselectedSeats = performanceSeatRepository.findAllByPerformanceIdAndParticipantNo(performanceId, participantNo)
            .filter { it.status == PerformanceSeatStatus.PRESELECTED }
        requireRule(preselectedSeats.isNotEmpty(), PerformanceErrorCode.NO_SEATS_SELECTED)

        val lapsedSeatIds = preselectedSeats.filterNot { it.isTakenAt(now) }.map { it.seatId }
        if (lapsedSeatIds.isNotEmpty()) {
            performanceSeatRepository.releaseLapsedPreselects(performanceId, participantNo, now)
            logger.info("선택 완료 실패(가선점 시간이 끝남) performanceId={} participantNo={} seatIds={}", performanceId, participantNo, lapsedSeatIds)
            return HoldSeatsResponse(heldSeatIds = emptyList(), unselectableSeatIds = lapsedSeatIds)
        }

        val holdExpiresAt = participant.hold(now, bookingProperties.holdDuration)
        val heldCount = performanceSeatRepository.hold(performanceId, participantNo, holdExpiresAt, now)
        check(heldCount == preselectedSeats.size) {
            "가선점한 좌석을 모두 선점하지 못했습니다. performanceId=$performanceId participantNo=$participantNo heldCount=$heldCount"
        }
        return HoldSeatsResponse(heldSeatIds = preselectedSeats.map { it.seatId }, unselectableSeatIds = emptyList())
    }

    // 좌석 선택 단계로 돌아간다. 선점한 좌석을 모두 풀고, 다시 고르는 첫 좌석부터 가선점 시간을 새로 센다.
    fun releaseHold(performanceId: Long, participantNo: Int) {
        val participant = getParticipantForUpdate(performanceId, participantNo)
        requireRule(!isPaying(performanceId, participantNo), PerformanceErrorCode.SEATS_PAYING)
        participant.clearHold()
        performanceSeatRepository.releaseHeld(performanceId, participantNo, LocalDateTime.now(clock))
    }

    fun isPaying(performanceId: Long, participantNo: Int): Boolean =
        performanceSeatRepository.existsByPerformanceIdAndParticipantNoAndStatus(performanceId, participantNo, PerformanceSeatStatus.PAYING)

    // 결제를 부르기 전에 선점 좌석을 결제 중으로 잠근다. 좌석 수만큼 바꾸지 못하면 호출한 쪽이 롤백한다.
    fun startPaying(performanceId: Long, participantNo: Int, seatCount: Int): Boolean =
        performanceSeatRepository.startPaying(performanceId, participantNo, LocalDateTime.now(clock)) == seatCount

    // 결제가 승인된 뒤 확정한다. 결제 전에 확인한 조건(예매 가능 시간, 공연 시작 시각)을 여기서 다시 확인한다.
    // 좌석 쪽 조건은 UPDATE의 WHERE(결제 중, 만료 전)가 확인한다.
    fun reserve(performanceId: Long, participantNo: Int, seatCount: Int): Boolean {
        val now = LocalDateTime.now(clock)
        val performance = performanceRepository.findByIdOrNull(performanceId) ?: return false
        if (performance.isStartedAt(now)) {
            return false
        }
        return performanceSeatRepository.reserve(performanceId, participantNo, now) == seatCount
    }

    // 결제가 승인되지 않았을 때 선점으로 되돌린다. 예매 가능 시간은 그대로 흐른다.
    fun revertPaying(performanceId: Long, participantNo: Int) {
        performanceSeatRepository.revertPaying(performanceId, participantNo, LocalDateTime.now(clock))
    }

    // 결제는 승인됐지만 확정하지 못했을 때 좌석을 푼다.
    fun releasePaying(performanceId: Long, participantNo: Int) {
        performanceSeatRepository.releasePaying(performanceId, participantNo, LocalDateTime.now(clock))
    }

    private fun getParticipantForUpdate(performanceId: Long, participantNo: Int): ParticipantEntity =
        participantRepository.findForUpdate(performanceId, participantNo) ?: throw BusinessException(PerformanceErrorCode.SESSION_EXPIRED)

    companion object {
        private val STATUSES_AFTER_HOLD = setOf(PerformanceSeatStatus.HELD, PerformanceSeatStatus.PAYING, PerformanceSeatStatus.RESERVED)
    }
}
