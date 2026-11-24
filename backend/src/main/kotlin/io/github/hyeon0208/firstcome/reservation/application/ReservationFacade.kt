package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.reservation.application.dto.CreateReservationRequest
import io.github.hyeon0208.firstcome.reservation.application.dto.PendingReservation
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationStart
import io.github.hyeon0208.firstcome.reservation.domain.ReservationErrorCode
import io.github.hyeon0208.firstcome.support.error.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ReservationFacade(
    private val reservationWriteService: ReservationWriteService,
    private val reservationReadService: ReservationReadService,
    private val paymentClient: PaymentClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun createReservation(request: CreateReservationRequest, idempotencyKey: String): ReservationResponse =
        when (val started = reservationWriteService.startReservation(request, idempotencyKey)) {
            is ReservationStart.AlreadyDone -> started.response
            is ReservationStart.Pending -> payAndConfirm(started.pending)
        }

    private fun payAndConfirm(pending: PendingReservation): ReservationResponse {
        val approval = pay(pending)
        try {
            reservationWriteService.confirmReservation(pending, approval)
        } catch (exception: Exception) {
            if (failIfStillPending(pending, approval.transactionId)) cancelPayment(pending)
            throw exception
        }
        return reservationReadService.getReservation(pending.reservationId)
    }

    private fun pay(pending: PendingReservation): PaymentApproval =
        try {
            paymentClient.pay(PaymentRequest(pending.reservationId, pending.amount))
        } catch (_: PaymentDeclinedException) {
            failIfStillPending(pending, null)
            throw BusinessException(ReservationErrorCode.PAYMENT_DECLINED)
        } catch (exception: Exception) {
            // 응답을 받지 못했을 뿐 승인됐을 수 있다. 취소가 확인된 뒤에만 좌석을 되돌려, 같은 임시 배정에 결제가 두 번 나가지 않게 한다.
            logger.error("결제 결과를 받지 못함 reservationId={}", pending.reservationId, exception)
            if (cancelPayment(pending)) failIfStillPending(pending, null)
            throw BusinessException(ReservationErrorCode.PAYMENT_UNAVAILABLE)
        }

    private fun failIfStillPending(pending: PendingReservation, paymentTransactionId: String?): Boolean =
        try {
            reservationWriteService.failIfPending(pending, paymentTransactionId)
        } catch (exception: Exception) {
            logger.error("예약 결과 확인 필요 reservationId={} paymentTransactionId={}", pending.reservationId, paymentTransactionId, exception)
            false
        }

    private fun cancelPayment(pending: PendingReservation): Boolean =
        try {
            paymentClient.cancel(pending.reservationId)
            logger.warn("예약을 확정하지 못해 결제를 취소함 reservationId={}", pending.reservationId)
            true
        } catch (exception: Exception) {
            logger.error("결제 취소 실패, 환불 필요 reservationId={}", pending.reservationId, exception)
            false
        }
}
