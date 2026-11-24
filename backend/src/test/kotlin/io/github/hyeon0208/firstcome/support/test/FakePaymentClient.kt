package io.github.hyeon0208.firstcome.support.test

import io.github.hyeon0208.firstcome.reservation.application.PaymentApproval
import io.github.hyeon0208.firstcome.reservation.application.PaymentClient
import io.github.hyeon0208.firstcome.reservation.application.PaymentDeclinedException
import io.github.hyeon0208.firstcome.reservation.application.PaymentRequest
import java.util.concurrent.CopyOnWriteArrayList

class FakePaymentClient : PaymentClient {
    val approvedRequests: MutableList<PaymentRequest> = CopyOnWriteArrayList()
    val cancelledTransactionIds: MutableList<String> = CopyOnWriteArrayList()

    @Volatile
    var beforeApprove: (() -> Unit)? = null

    @Volatile
    var declinesPayment: Boolean = false

    @Volatile
    var failsToCancel: Boolean = false

    override fun pay(request: PaymentRequest): PaymentApproval {
        beforeApprove?.invoke()
        if (declinesPayment) throw PaymentDeclinedException("결제가 거절되었습니다.")
        approvedRequests += request
        return PaymentApproval(transactionId = "FAKE-${request.reservationId}")
    }

    override fun cancel(transactionId: String) {
        if (failsToCancel) throw IllegalStateException("결제 취소에 실패했습니다.")
        cancelledTransactionIds += transactionId
    }

    fun reset() {
        approvedRequests.clear()
        cancelledTransactionIds.clear()
        beforeApprove = null
        declinesPayment = false
        failsToCancel = false
    }
}
