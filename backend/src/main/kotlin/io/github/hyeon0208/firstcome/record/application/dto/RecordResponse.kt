package io.github.hyeon0208.firstcome.record.application.dto

import io.github.hyeon0208.firstcome.performance.Difficulty
import io.github.hyeon0208.firstcome.performance.PerformanceResult
import io.github.hyeon0208.firstcome.record.domain.RecordEntity
import java.time.LocalDateTime

data class RecordResponse(
    val id: Long,
    val performanceId: Long,
    val showTitle: String,
    val difficulty: Difficulty,
    val result: PerformanceResult,
    val elapsedSeconds: Long,
    val endedAt: LocalDateTime,
) {
    companion object {
        fun from(record: RecordEntity): RecordResponse =
            RecordResponse(
                id = record.id,
                performanceId = record.performanceId,
                showTitle = record.showTitle,
                difficulty = record.difficulty,
                result = record.result,
                elapsedSeconds = record.elapsedSeconds,
                endedAt = record.endedAt,
            )
    }
}
