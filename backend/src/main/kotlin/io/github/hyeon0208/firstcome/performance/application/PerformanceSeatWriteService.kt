package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.application.dto.HoldSeatsRequest
import io.github.hyeon0208.firstcome.performance.application.dto.HoldSeatsResponse
import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceSeatResponse
import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatEntity
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatStatus
import io.github.hyeon0208.firstcome.performance.domain.SeatSelection
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceRepository
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceSeatRepository
import io.github.hyeon0208.firstcome.show.domain.ShowSeatPrices
import io.github.hyeon0208.firstcome.show.infrastructure.JpaShowRepository
import io.github.hyeon0208.firstcome.show.infrastructure.JpaShowSeatPriceRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.user.domain.UserErrorCode
import io.github.hyeon0208.firstcome.user.infrastructure.JpaUserRepository
import io.github.hyeon0208.firstcome.show.infrastructure.JpaSeatRepository
import java.time.Clock
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PerformanceSeatWriteService(
    private val userRepository: JpaUserRepository,
    private val showRepository: JpaShowRepository,
    private val performanceRepository: JpaPerformanceRepository,
    private val showSeatPriceRepository: JpaShowSeatPriceRepository,
    private val seatRepository: JpaSeatRepository,
    private val performanceSeatRepository: JpaPerformanceSeatRepository,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun holdSeats(venueId: Long, showId: Long, performanceId: Long, request: HoldSeatsRequest): HoldSeatsResponse {
        val selection = SeatSelection(request.performanceSeatIds)
        val show = showRepository.findByIdAndVenueId(showId, venueId)
            ?: throw BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND)
        val performance = performanceRepository.findByIdAndShowId(performanceId, showId)
            ?: throw BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND)
        val now = LocalDateTime.now(clock)
        if (performance.isStartedAt(now)) {
            throw BusinessException(PerformanceErrorCode.PERFORMANCE_ALREADY_STARTED)
        }

        val performanceSeats = performanceSeatRepository.findAllById(selection.performanceSeatIds)
        if (performanceSeats.size != selection.performanceSeatIds.size || performanceSeats.any { it.performanceId != performance.id }) {
            throw BusinessException(PerformanceErrorCode.SEAT_NOT_FOUND)
        }
        val seatsById = seatRepository.findAllById(performanceSeats.map { it.seatId }).associateBy { it.id }

        userRepository.findByIdForUpdate(request.userId) ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)
        if (isPaying(performance.id, request.userId)) {
            throw BusinessException(PerformanceErrorCode.SEATS_PAYING)
        }
        val heldSeatIds = performanceSeatRepository.findHeldSeats(performance.id, request.userId).map { it.id }
        performanceSeatRepository.findAllByIdInOrderByIdForUpdate((heldSeatIds + selection.performanceSeatIds).distinct())
        val releasedCount = performanceSeatRepository.releaseHolds(performance.id, request.userId, now)
        val holdExpiresAt = now.plus(PerformanceSeatEntity.HOLD_DURATION)
        val heldCount = performanceSeatRepository.holdSeats(selection.performanceSeatIds, performance.id, request.userId, holdExpiresAt, now)
        if (heldCount != selection.performanceSeatIds.size) {
            logger.info(
                "좌석 임시 배정 실패(다른 사용자가 임시 배정했거나 예약된 좌석 포함) userId={} performanceId={} performanceSeatIds={} heldCount={}",
                request.userId,
                performance.id,
                selection.performanceSeatIds,
                heldCount,
            )
            throw BusinessException(PerformanceErrorCode.SEAT_NOT_AVAILABLE)
        }
        logger.info(
            "좌석 임시 배정 userId={} performanceId={} performanceSeatIds={} releasedCount={} holdExpiresAt={}",
            request.userId,
            performance.id,
            selection.performanceSeatIds,
            releasedCount,
            holdExpiresAt,
        )

        val prices = ShowSeatPrices(show.id, showSeatPriceRepository.findAllByShowId(show.id))
        val seats = performanceSeats.sortedBy { it.id }.map { performanceSeat ->
            val seat = seatsById.getValue(performanceSeat.seatId)
            PerformanceSeatResponse.of(performanceSeat, seat, prices.priceOf(seat.grade), PerformanceSeatStatus.HELD)
        }
        return HoldSeatsResponse.of(performance.id, seats, holdExpiresAt)
    }

    fun isPaying(performanceId: Long, holderId: Long): Boolean =
        performanceSeatRepository.existsByPerformanceIdAndHolderIdAndStatus(performanceId, holderId, PerformanceSeatStatus.PAYING)

    // 결제를 부르기 전에 임시 배정 좌석을 결제 중으로 잠근다. 전부 바꾸지 못하면 아무것도 바꾸지 않은 것으로 본다(호출한 쪽이 롤백한다).
    fun startPaying(performanceSeatIds: List<Long>, holderId: Long, now: LocalDateTime): Boolean =
        performanceSeatRepository.startPaying(performanceSeatIds, holderId, now) == performanceSeatIds.size

    // 결제가 승인된 뒤 확정한다. 결제 중이고 만료 시각이 지나지 않았을 때만 바꾼다.
    fun reserveSeats(performanceSeatIds: List<Long>, holderId: Long): Boolean =
        performanceSeatRepository.reserveSeats(performanceSeatIds, holderId, LocalDateTime.now(clock)) == performanceSeatIds.size

    // 결제가 승인되지 않았을 때 임시 배정으로 되돌린다. 만료 시각은 그대로다.
    fun revertPaying(performanceSeatIds: List<Long>, holderId: Long) {
        performanceSeatRepository.revertPaying(performanceSeatIds, holderId, LocalDateTime.now(clock))
    }

    // 결제는 승인됐지만 확정하지 못했을 때 좌석을 푼다.
    fun releasePaying(performanceSeatIds: List<Long>, holderId: Long) {
        performanceSeatRepository.releasePaying(performanceSeatIds, holderId, LocalDateTime.now(clock))
    }
}
