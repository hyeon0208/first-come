package io.github.hyeon0208.firstcome.paymentmock

data class ApproveRequest(
    val orderId: Long,
    val amount: Long,
)
