package io.github.hyeon0208.firstcome.performance

import java.time.LocalDateTime

// 회차가 만들어졌다. 가상 경쟁자는 예매 오픈 시각부터 움직인다.
data class PerformanceCreated(
    val performanceId: Long,
    val showId: Long,
    val difficulty: Difficulty,
    val seed: Long,
    val openAt: LocalDateTime,
)
