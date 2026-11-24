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
            if (failIfStillPending(pending, approval)) cancelPayment(pending, approval)
            throw exception
        }
        return reservationReadService.getReservation(pending.reservationId)
    }

    private fun pay(pending: PendingReservation): PaymentApproval =
        try {
            paymentClient.pay(PaymentRequest(pending.reservationId, pending.amount))
        } catch (_: PaymentDeclinedException) {
            reservationWriteService.failIfPending(pending.reservationId, null)
            throw BusinessException(ReservationErrorCode.PAYMENT_DECLINED)
        }

    private fun failIfStillPending(pending: PendingReservation, approval: PaymentApproval): Boolean =
        try {
            reservationWriteService.failIfPending(pending.reservationId, approval.transactionId)
        } catch (exception: Exception) {
            logger.error("예약 확정 결과 확인 필요 reservationId={} paymentTransactionId={}", pending.reservationId, approval.transactionId, exception)
            false
        }

    private fun cancelPayment(pending: PendingReservation, approval: PaymentApproval) {
        try {
            paymentClient.cancel(approval.transactionId)
            logger.warn("예약을 확정하지 못해 결제를 취소함 reservationId={} paymentTransactionId={}", pending.reservationId, approval.transactionId)
        } catch (exception: Exception) {
            logger.error("결제 취소 실패, 환불 필요 reservationId={} paymentTransactionId={}", pending.reservationId, approval.transactionId, exception)
        }
    }
}
