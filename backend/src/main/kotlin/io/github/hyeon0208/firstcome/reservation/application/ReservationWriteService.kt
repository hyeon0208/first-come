package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.performance.application.PerformanceSeatWriteService
import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatEntity
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatStatus
import io.github.hyeon0208.firstcome.performance.domain.SeatSelection
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceRepository
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceSeatRepository
import io.github.hyeon0208.firstcome.reservation.application.dto.CreateReservationRequest
import io.github.hyeon0208.firstcome.reservation.application.dto.PendingReservation
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationStart
import io.github.hyeon0208.firstcome.reservation.domain.ReservationEntity
import io.github.hyeon0208.firstcome.reservation.domain.ReservationErrorCode
import io.github.hyeon0208.firstcome.reservation.domain.ReservationSeatEntity
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationRepository
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationSeatRepository
import io.github.hyeon0208.firstcome.show.domain.ShowSeatPrices
import io.github.hyeon0208.firstcome.show.infrastructure.JpaShowSeatPriceRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.user.domain.UserEntity
import io.github.hyeon0208.firstcome.user.domain.UserErrorCode
import io.github.hyeon0208.firstcome.user.infrastructure.JpaUserRepository
import io.github.hyeon0208.firstcome.show.infrastructure.JpaSeatRepository
import java.time.Clock
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReservationWriteService(
    private val reservationRepository: JpaReservationRepository,
    private val reservationSeatRepository: JpaReservationSeatRepository,
    private val reservationReadService: ReservationReadService,
    private val userRepository: JpaUserRepository,
    private val performanceRepository: JpaPerformanceRepository,
    private val showSeatPriceRepository: JpaShowSeatPriceRepository,
    private val seatRepository: JpaSeatRepository,
    private val performanceSeatRepository: JpaPerformanceSeatRepository,
    private val performanceSeatWriteService: PerformanceSeatWriteService,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun startReservation(request: CreateReservationRequest, idempotencyKey: String): ReservationStart {
        val user = userRepository.findByIdForUpdate(request.userId) ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)
        findPreviousResult(request, idempotencyKey)?.let { return ReservationStart.AlreadyDone(it) }
        return ReservationStart.Pending(createPendingReservation(request, user, idempotencyKey))
    }

    private fun findPreviousResult(request: CreateReservationRequest, idempotencyKey: String): ReservationResponse? {
        val reservation = reservationRepository.findByUserIdAndIdempotencyKey(request.userId, idempotencyKey) ?: return null
        val requestedSeatIds = SeatSelection(request.performanceSeatIds).performanceSeatIds.toSet()
        val reservedSeatIds = reservationSeatRepository.findAllByReservationId(reservation.id).map { it.performanceSeatId }.toSet()
        val isSameRequest = reservation.performanceId == request.performanceId && reservedSeatIds == requestedSeatIds
        if (!isSameRequest) {
            throw BusinessException(ReservationErrorCode.IDEMPOTENCY_KEY_MISMATCH)
        }

        logger.info("같은 멱등 키 재요청 reservationId={} userId={} status={}", reservation.id, request.userId, reservation.status)
        return when (reservation.status) {
            ReservationStatus.PENDING -> throw BusinessException(ReservationErrorCode.IDEMPOTENCY_KEY_IN_PROGRESS)
            ReservationStatus.FAILED -> throw BusinessException(ReservationErrorCode.IDEMPOTENCY_KEY_ALREADY_FAILED)
            ReservationStatus.CONFIRMED -> reservationReadService.getReservation(reservation.id)
        }
    }

    private fun createPendingReservation(request: CreateReservationRequest, user: UserEntity, idempotencyKey: String): PendingReservation {
        val selection = SeatSelection(request.performanceSeatIds)
        val performance = performanceRepository.findByIdOrNull(request.performanceId)
            ?: throw BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND)
        val now = LocalDateTime.now(clock)
        if (performance.isStartedAt(now)) {
            throw BusinessException(PerformanceErrorCode.PERFORMANCE_ALREADY_STARTED)
        }

        val heldSeats = findHeldSeatsMatching(user.id, performance.id, selection, now)
        val priceByPerformanceSeatId = priceByPerformanceSeatId(heldSeats, performance.showId)
        val amount = priceByPerformanceSeatId.values.sum()

        val reservation = reservationRepository.save(
            ReservationEntity(
                userId = user.id,
                performanceId = performance.id,
                idempotencyKey = idempotencyKey,
                amount = amount,
            ),
        )

        reservationSeatRepository.saveAll(
            priceByPerformanceSeatId.map { (performanceSeatId, seatPrice) ->
                ReservationSeatEntity(reservationId = reservation.id, performanceId = performance.id, performanceSeatId = performanceSeatId, price = seatPrice)
            },
        )
        logger.info(
            "예약 결제 대기 저장 reservationId={} userId={} performanceId={} performanceSeatIds={} amount={}",
            reservation.id,
            user.id,
            performance.id,
            priceByPerformanceSeatId.keys,
            amount,
        )
        return PendingReservation(
            reservationId = reservation.id,
            userId = user.id,
            performanceSeatIds = priceByPerformanceSeatId.keys.toList(),
            amount = amount,
        )
    }

    private fun findHeldSeatsMatching(userId: Long, performanceId: Long, selection: SeatSelection, now: LocalDateTime): List<PerformanceSeatEntity> {
        val heldSeats = performanceSeatRepository.findHeldSeats(performanceId, userId)
        val heldSeatIds = heldSeats.map { it.id }.toSet()
        val holdError = when {
            heldSeats.isEmpty() -> ReservationErrorCode.HOLD_NOT_FOUND
            heldSeats.any { it.statusAt(now) != PerformanceSeatStatus.HELD } -> ReservationErrorCode.HOLD_EXPIRED
            heldSeatIds != selection.performanceSeatIds.toSet() -> ReservationErrorCode.HOLD_MISMATCH
            else -> return heldSeats
        }
        logger.info(
            "예약 확정 거부 code={} userId={} performanceId={} requestedSeatIds={} heldSeatIds={}",
            holdError.name,
            userId,
            performanceId,
            selection.performanceSeatIds,
            heldSeatIds,
        )
        throw BusinessException(holdError)
    }

    private fun priceByPerformanceSeatId(heldSeats: List<PerformanceSeatEntity>, showId: Long): Map<Long, Long> {
        val gradeBySeatId = seatRepository.findAllById(heldSeats.map { it.seatId }).associate { it.id to it.grade }
        val prices = ShowSeatPrices(showId, showSeatPriceRepository.findAllByShowId(showId))
        return heldSeats.associate { it.id to prices.priceOf(gradeBySeatId.getValue(it.seatId)) }
    }

    fun confirmReservation(pending: PendingReservation, approval: PaymentApproval) {
        if (!performanceSeatWriteService.reserveSeats(pending.performanceSeatIds, pending.userId)) {
            logger.info(
                "예약 확정 실패(결제 중 임시 배정이 풀림) reservationId={} performanceSeatIds={}",
                pending.reservationId,
                pending.performanceSeatIds,
            )
            throw BusinessException(ReservationErrorCode.HOLD_LOST)
        }

        reservationSeatRepository.findAllByReservationId(pending.reservationId).forEach { it.activate() }
        val reservation = checkNotNull(reservationRepository.findByIdOrNull(pending.reservationId)) { "예약이 없습니다. reservationId=${pending.reservationId}" }
        reservation.confirm(approval.transactionId, LocalDateTime.now(clock))
        logger.info("예약 확정 reservationId={} paymentTransactionId={}", pending.reservationId, approval.transactionId)
    }

    fun failIfPending(reservationId: Long, paymentTransactionId: String?): Boolean {
        val reservation = checkNotNull(reservationRepository.findByIdForUpdate(reservationId)) { "예약이 없습니다. reservationId=$reservationId" }
        if (reservation.status != ReservationStatus.PENDING) {
            return false
        }
        reservation.fail(paymentTransactionId)
        logger.info("예약 실패 처리 reservationId={} paymentTransactionId={}", reservationId, paymentTransactionId)
        return true
    }
}
