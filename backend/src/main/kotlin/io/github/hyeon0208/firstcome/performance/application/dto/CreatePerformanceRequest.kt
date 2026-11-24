package io.github.hyeon0208.firstcome.performance.application.dto

import io.github.hyeon0208.firstcome.performance.Difficulty

data class CreatePerformanceRequest(
    val showId: Long,
    val difficulty: Difficulty,
)
