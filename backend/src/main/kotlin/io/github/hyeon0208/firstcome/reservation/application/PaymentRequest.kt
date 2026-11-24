package io.github.hyeon0208.firstcome.reservation.application

data class PaymentRequest(
    val reservationId: Long,
    val amount: Long,
)
