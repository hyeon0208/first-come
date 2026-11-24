package io.github.hyeon0208.firstcome.support.test

import io.github.hyeon0208.firstcome.reservation.application.PaymentApproval
import io.github.hyeon0208.firstcome.reservation.application.PaymentClient
import io.github.hyeon0208.firstcome.reservation.application.PaymentDeclinedException
import io.github.hyeon0208.firstcome.reservation.application.PaymentRequest
import java.util.concurrent.CopyOnWriteArrayList

class FakePaymentClient : PaymentClient {
    val approvedRequests: MutableList<PaymentRequest> = CopyOnWriteArrayList()
    val cancelledReservationIds: MutableList<Long> = CopyOnWriteArrayList()

    @Volatile
    var beforeApprove: (() -> Unit)? = null

    @Volatile
    var declinesPayment: Boolean = false

    // 승인은 했는데 응답이 돌아가지 않는 상황
    @Volatile
    var losesResponse: Boolean = false

    @Volatile
    var failsToCancel: Boolean = false

    override fun pay(request: PaymentRequest): PaymentApproval {
        beforeApprove?.invoke()
        if (declinesPayment) throw PaymentDeclinedException("결제가 거절되었습니다.")
        approvedRequests += request
        if (losesResponse) throw IllegalStateException("결제 응답을 받지 못했습니다.")
        return PaymentApproval(transactionId = "FAKE-${request.reservationId}")
    }

    override fun cancel(reservationId: Long) {
        if (failsToCancel) throw IllegalStateException("결제 취소에 실패했습니다.")
        cancelledReservationIds += reservationId
    }

    fun reset() {
        approvedRequests.clear()
        cancelledReservationIds.clear()
        beforeApprove = null
        declinesPayment = false
        losesResponse = false
        failsToCancel = false
    }
}
