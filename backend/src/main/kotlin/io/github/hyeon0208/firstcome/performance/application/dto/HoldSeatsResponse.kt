package io.github.hyeon0208.firstcome.performance.application.dto

import java.time.LocalDateTime

data class HoldSeatsResponse(
    val performanceId: Long,
    val seats: List<PerformanceSeatResponse>,
    val totalPrice: Long,
    val holdExpiresAt: LocalDateTime,
) {
    companion object {
        fun of(performanceId: Long, seats: List<PerformanceSeatResponse>, holdExpiresAt: LocalDateTime): HoldSeatsResponse =
            HoldSeatsResponse(
                performanceId = performanceId,
                seats = seats,
                totalPrice = seats.sumOf { it.price },
                holdExpiresAt = holdExpiresAt,
            )
    }
}
