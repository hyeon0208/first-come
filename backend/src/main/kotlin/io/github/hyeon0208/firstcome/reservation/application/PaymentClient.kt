package io.github.hyeon0208.firstcome.reservation.application

interface PaymentClient {
    fun pay(request: PaymentRequest): PaymentApproval

    fun cancel(transactionId: String)
}
