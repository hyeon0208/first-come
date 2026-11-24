package io.github.hyeon0208.firstcome.reservation.application.dto

data class CreateReservationRequest(
    val userId: Long,
    val performanceId: Long,
    val performanceSeatIds: List<Long>,
)
