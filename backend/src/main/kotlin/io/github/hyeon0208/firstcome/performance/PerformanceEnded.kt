package io.github.hyeon0208.firstcome.performance

import java.time.LocalDateTime

// 회차가 끝났다. 걸린 시간은 예매 오픈 시각부터 끝난 순간까지다(오픈 전에 끝나면 0초).
data class PerformanceEnded(
    val performanceId: Long,
    val userId: Long,
    val showId: Long,
    val difficulty: Difficulty,
    val result: PerformanceResult,
    val elapsedSeconds: Long,
    val endedAt: LocalDateTime,
)
