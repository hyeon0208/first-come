package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.performance.ParticipantNo
import io.github.hyeon0208.firstcome.performance.application.BookingFacade
import io.github.hyeon0208.firstcome.reservation.application.dto.PendingReservation
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationStart
import io.github.hyeon0208.firstcome.reservation.domain.ReservationErrorCode
import io.github.hyeon0208.firstcome.support.error.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

// 결제는 느린 외부 호출이라 트랜잭션 밖에서 부른다. 그래서 세 단계로 나눈다.
// 1. 짧은 트랜잭션: 선점한 좌석을 결제 중으로 잠근다(같은 선점에 결제가 두 번 나가지 않게).
// 2. 트랜잭션 없이 결제를 부른다.
// 3. 짧은 트랜잭션: 결제 전에 확인한 조건을 다시 확인하고 확정한다. 어긋나면 결제를 취소한다.
@Service
class ReservationFacade(
    private val reservationWriteService: ReservationWriteService,
    private val reservationReadService: ReservationReadService,
    private val bookingFacade: BookingFacade,
    private val paymentClient: PaymentClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // 사용자의 결제 요청.
    fun reserve(userId: Long, performanceId: Long, idempotencyKey: String): ReservationResponse {
        bookingFacade.requireActive(userId, performanceId)
        return reserve(performanceId, ParticipantNo.USER, idempotencyKey)
    }

    // 한 참가자가 선점한 좌석 전체를 결제해 예매한다. 사용자와 가상 경쟁자가 같은 경로를 쓴다.
    fun reserve(performanceId: Long, participantNo: Int, idempotencyKey: String): ReservationResponse =
        when (val started = reservationWriteService.startReservation(performanceId, participantNo, idempotencyKey)) {
            is ReservationStart.AlreadyDone -> started.response
            is ReservationStart.Pending -> payAndConfirm(started.pending)
        }

    private fun payAndConfirm(pending: PendingReservation): ReservationResponse {
        val approval = pay(pending)
        try {
            reservationWriteService.confirmReservation(pending, approval)
        } catch (exception: Exception) {
            val timedOut = (exception as? BusinessException)?.errorCode == ReservationErrorCode.BOOKING_TIME_EXPIRED
            if (failIfStillPending(pending, approval.transactionId, timedOut)) cancelPayment(pending)
            throw exception
        }
        return reservationReadService.getReservation(pending.reservationId)
    }

    private fun pay(pending: PendingReservation): PaymentApproval =
        try {
            paymentClient.pay(PaymentRequest(pending.reservationId, pending.amount))
        } catch (_: PaymentDeclinedException) {
            failIfStillPending(pending, null, timedOut = false)
            throw BusinessException(ReservationErrorCode.PAYMENT_DECLINED)
        } catch (exception: Exception) {
            // 응답을 받지 못했을 뿐 승인됐을 수 있다. 취소가 확인된 뒤에만 좌석을 되돌려, 같은 선점에 결제가 두 번 나가지 않게 한다.
            // 취소도 실패하면 좌석은 결제 중으로 남고, 회차가 끝날 때 정리된다.
            logger.error("결제 결과를 받지 못함 reservationId={}", pending.reservationId, exception)
            if (cancelPayment(pending)) failIfStillPending(pending, null, timedOut = false)
            throw BusinessException(ReservationErrorCode.PAYMENT_UNAVAILABLE)
        }

    private fun failIfStillPending(pending: PendingReservation, paymentTransactionId: String?, timedOut: Boolean): Boolean =
        try {
            reservationWriteService.failIfPending(pending, paymentTransactionId, timedOut)
        } catch (exception: Exception) {
            logger.error("예매 결과 확인 필요 reservationId={} paymentTransactionId={}", pending.reservationId, paymentTransactionId, exception)
            false
        }

    private fun cancelPayment(pending: PendingReservation): Boolean =
        try {
            paymentClient.cancel(pending.reservationId)
            logger.warn("예매를 확정하지 못해 결제를 취소함 reservationId={}", pending.reservationId)
            true
        } catch (exception: Exception) {
            logger.error("결제 취소 실패, 환불 필요 reservationId={}", pending.reservationId, exception)
            false
        }
}
