package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.performance.BookingPhase
import io.github.hyeon0208.firstcome.performance.ParticipantNo
import io.github.hyeon0208.firstcome.performance.PerformancePurged
import io.github.hyeon0208.firstcome.performance.PerformanceResult
import io.github.hyeon0208.firstcome.performance.application.PerformanceReadService
import io.github.hyeon0208.firstcome.performance.application.PerformanceSeatReadService
import io.github.hyeon0208.firstcome.performance.application.PerformanceSeatWriteService
import io.github.hyeon0208.firstcome.performance.application.PerformanceWriteService
import io.github.hyeon0208.firstcome.reservation.application.dto.PendingReservation
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationStart
import io.github.hyeon0208.firstcome.reservation.domain.ReservationEntity
import io.github.hyeon0208.firstcome.reservation.domain.ReservationErrorCode
import io.github.hyeon0208.firstcome.reservation.domain.ReservationSeatEntity
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationRepository
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationSeatRepository
import io.github.hyeon0208.firstcome.show.application.ShowReadService
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.error.requireRule
import java.time.Clock
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 예매의 세 단계 중 DB를 바꾸는 두 단계(결제 전 잠금, 결제 뒤 확정)와 실패 처리. 결제 호출은 ReservationFacade가 트랜잭션 밖에서 한다.
@Service
@Transactional
class ReservationWriteService(
    private val reservationRepository: JpaReservationRepository,
    private val reservationSeatRepository: JpaReservationSeatRepository,
    private val reservationReadService: ReservationReadService,
    private val performanceReadService: PerformanceReadService,
    private val performanceWriteService: PerformanceWriteService,
    private val performanceSeatReadService: PerformanceSeatReadService,
    private val performanceSeatWriteService: PerformanceSeatWriteService,
    private val showReadService: ShowReadService,
    private val reservationProperties: ReservationProperties,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // 1단계: 선점한 좌석을 결제 중으로 잠그고 결제 대기 예매를 저장한다.
    // 참가자 행을 잠가 같은 참가자의 요청을 한 줄로 세운 뒤, 같은 트랜잭션에서 멱등 키와 좌석 상태를 확인한다.
    fun startReservation(performanceId: Long, participantNo: Int, idempotencyKey: String): ReservationStart {
        performanceSeatWriteService.lockParticipant(performanceId, participantNo)
        findPreviousResult(performanceId, participantNo, idempotencyKey)?.let { return ReservationStart.AlreadyDone(it) }

        val heldSeats = performanceSeatReadService.getParticipantSeats(performanceId, participantNo)
        // 같은 선점에는 결제가 한 번에 한 건만 진행된다. 멱등 키가 달라도 결제를 부르지 않고 거절한다.
        requireRule(heldSeats.phase != BookingPhase.PAYING, ReservationErrorCode.PAYMENT_IN_PROGRESS)
        requireRule(heldSeats.phase == BookingPhase.HELD, ReservationErrorCode.HOLD_NOT_FOUND)
        requireRule(!heldSeats.expired, ReservationErrorCode.HOLD_EXPIRED)
        val performance = performanceReadService.findPerformance(performanceId)?.takeIf { it.active }
            ?: throw BusinessException(ReservationErrorCode.SESSION_EXPIRED)
        requireRule(performance.serverNow.isBefore(performance.startAt), ReservationErrorCode.PERFORMANCE_ALREADY_STARTED)
        val priceBySeatId = showReadService.getSeats(performance.showId).filter { it.seatId in heldSeats.seatIds }.associate { it.seatId to it.price }
        // 좌석 수만큼 바꾸지 못했다면 그사이 다른 요청이 먼저 잠근 것이다.
        requireRule(
            performanceSeatWriteService.startPaying(performanceId, participantNo, heldSeats.seatIds.size),
            ReservationErrorCode.PAYMENT_IN_PROGRESS,
        )

        val reservation = reservationRepository.save(
            ReservationEntity(
                performanceId = performanceId,
                participantNo = participantNo,
                idempotencyKey = idempotencyKey,
                ticketAmount = priceBySeatId.values.sum(),
                feeAmount = reservationProperties.feePerTicket * priceBySeatId.size,
            ),
        )
        reservationSeatRepository.saveAll(
            priceBySeatId.map { (seatId, price) ->
                ReservationSeatEntity(reservationId = reservation.id, performanceId = performanceId, seatId = seatId, price = price)
            },
        )
        logger.info(
            "예매 결제 대기 저장 reservationId={} performanceId={} participantNo={} seatIds={} amount={}",
            reservation.id,
            performanceId,
            participantNo,
            priceBySeatId.keys,
            reservation.amount(),
        )
        return ReservationStart.Pending(
            PendingReservation(
                reservationId = reservation.id,
                performanceId = performanceId,
                participantNo = participantNo,
                seatCount = priceBySeatId.size,
                amount = reservation.amount(),
            ),
        )
    }

    // 3단계: 결제가 승인된 뒤 확정한다. 예매 가능 시간과 공연 시작 시각을 좌석 확정 단계가 다시 확인하고,
    // 어긋나면 확정하지 않는다(ReservationFacade가 결제를 취소한다).
    fun confirmReservation(pending: PendingReservation, approval: PaymentApproval) {
        if (!performanceSeatWriteService.reserve(pending.performanceId, pending.participantNo, pending.seatCount)) {
            logger.info("예매 확정 실패(결제 중 예매 가능 시간이 끝났거나 공연이 시작됨) reservationId={}", pending.reservationId)
            throw BusinessException(ReservationErrorCode.BOOKING_TIME_EXPIRED)
        }

        // 좌석 UPDATE가 영속성 컨텍스트를 비우므로 예매는 그 뒤에 읽는다.
        reservationSeatRepository.findAllByReservationId(pending.reservationId).forEach { it.activate() }
        val reservation = checkNotNull(reservationRepository.findByIdOrNull(pending.reservationId)) { "예매가 없습니다. reservationId=${pending.reservationId}" }
        reservation.confirm(approval.transactionId, LocalDateTime.now(clock))
        if (pending.participantNo == ParticipantNo.USER) {
            performanceWriteService.end(pending.performanceId, PerformanceResult.SUCCESS)
        }
        logger.info("예매 확정 reservationId={} paymentTransactionId={}", pending.reservationId, approval.transactionId)
    }

    // 결제 대기로 남아 있을 때만 실패로 바꾼다(이미 확정됐으면 false).
    // 결제가 승인되지 않았으면(승인 번호 없음) 좌석을 선점으로 되돌려 다시 결제할 수 있게 하고,
    // 승인됐는데 확정하지 못했으면 좌석을 푼다. 시간 초과였다면 사용자의 회차를 시간 초과로 끝낸다.
    fun failIfPending(pending: PendingReservation, paymentTransactionId: String?, timedOut: Boolean): Boolean {
        val reservation = reservationRepository.findByIdForUpdate(pending.reservationId)
        if (reservation == null) {
            // 회차가 먼저 지워졌다. 되돌릴 좌석도 예매도 없다.
            return true
        }
        if (reservation.status != ReservationStatus.PENDING) {
            return false
        }
        reservation.fail(paymentTransactionId)
        if (paymentTransactionId == null) {
            performanceSeatWriteService.revertPaying(pending.performanceId, pending.participantNo)
        } else {
            performanceSeatWriteService.releasePaying(pending.performanceId, pending.participantNo)
        }
        if (timedOut && pending.participantNo == ParticipantNo.USER) {
            performanceWriteService.end(pending.performanceId, PerformanceResult.TIMEOUT)
        }
        logger.info("예매 실패 처리 reservationId={} paymentTransactionId={} timedOut={}", pending.reservationId, paymentTransactionId, timedOut)
        return true
    }

    // 회차의 좌석 데이터가 지워질 때 그 회차의 예매도 함께 지운다. 모듈 사이에 FK가 없어 여기서 맞춘다.
    @EventListener
    fun deleteReservationsOf(event: PerformancePurged) {
        reservationSeatRepository.deleteAllByPerformanceId(event.performanceId)
        reservationRepository.deleteAllByPerformanceId(event.performanceId)
    }

    private fun findPreviousResult(performanceId: Long, participantNo: Int, idempotencyKey: String): ReservationResponse? {
        val reservation = reservationRepository.findByPerformanceIdAndParticipantNoAndIdempotencyKey(performanceId, participantNo, idempotencyKey)
            ?: return null
        logger.info("같은 멱등 키 재요청 reservationId={} status={}", reservation.id, reservation.status)
        return when (reservation.status) {
            ReservationStatus.PENDING -> throw BusinessException(ReservationErrorCode.IDEMPOTENCY_KEY_IN_PROGRESS)
            ReservationStatus.FAILED -> throw BusinessException(ReservationErrorCode.IDEMPOTENCY_KEY_ALREADY_FAILED)
            ReservationStatus.CONFIRMED -> reservationReadService.getReservation(reservation.id)
        }
    }
}
