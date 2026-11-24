package io.github.hyeon0208.firstcome.performance.application.dto

import io.github.hyeon0208.firstcome.performance.domain.PerformanceEntity
import io.github.hyeon0208.firstcome.show.domain.ShowEntity
import java.time.LocalDateTime

data class PerformanceSeatsResponse(
    val performanceId: Long,
    val showTitle: String,
    val startAt: LocalDateTime,
    val seats: List<PerformanceSeatResponse>,
) {
    companion object {
        fun of(
            show: ShowEntity,
            performance: PerformanceEntity,
            seats: List<PerformanceSeatResponse>,
        ): PerformanceSeatsResponse =
            PerformanceSeatsResponse(
                performanceId = performance.id,
                showTitle = show.title,
                startAt = performance.startAt,
                seats = seats,
            )
    }
}
