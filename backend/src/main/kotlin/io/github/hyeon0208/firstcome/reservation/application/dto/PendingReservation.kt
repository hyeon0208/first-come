package io.github.hyeon0208.firstcome.reservation.application.dto

data class PendingReservation(
    val reservationId: Long,
    val userId: Long,
    val performanceId: Long,
    val performanceSeatIds: List<Long>,
    val amount: Long,
)
