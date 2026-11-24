package io.github.hyeon0208.firstcome.show.application.dto

import io.github.hyeon0208.firstcome.performance.domain.PerformanceEntity
import java.time.LocalDateTime

data class PerformanceResponse(
    val id: Long,
    val startAt: LocalDateTime,
) {
    companion object {
        fun from(performance: PerformanceEntity): PerformanceResponse =
            PerformanceResponse(
                id = performance.id,
                startAt = performance.startAt,
            )
    }
}
