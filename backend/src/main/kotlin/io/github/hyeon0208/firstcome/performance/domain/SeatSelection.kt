package io.github.hyeon0208.firstcome.performance.domain

import io.github.hyeon0208.firstcome.support.error.requireRule

class SeatSelection(
    val performanceSeatIds: List<Long>,
) {
    init {
        requireRule(performanceSeatIds.size in 1..MAX_SEAT_COUNT, PerformanceErrorCode.SEAT_COUNT_OUT_OF_RANGE)
        requireRule(performanceSeatIds.toSet().size == performanceSeatIds.size, PerformanceErrorCode.DUPLICATED_SEAT)
    }

    companion object {
        const val MAX_SEAT_COUNT = 5
    }
}
