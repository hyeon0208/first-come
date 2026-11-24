package io.github.hyeon0208.firstcome.performance.application.dto

data class HoldSeatsRequest(
    val userId: Long,
    val performanceSeatIds: List<Long>,
)
