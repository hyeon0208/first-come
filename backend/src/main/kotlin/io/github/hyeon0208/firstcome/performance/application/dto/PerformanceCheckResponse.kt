package io.github.hyeon0208.firstcome.performance.application.dto

// 사용자의 요청이 닿은 회차가 지금 어떤 상태인지. 진행 중일 때만 회차 정보가 있다.
data class PerformanceCheckResponse(
    val state: State,
    val performance: PerformanceResponse?,
) {
    enum class State {
        ACTIVE,

        // 제한 시간이 지나 끝났다
        TIMED_OUT,

        // 없거나, 남의 것이거나, 다른 이유로 이미 끝났다
        GONE,
    }

    companion object {
        fun active(performance: PerformanceResponse): PerformanceCheckResponse =
            PerformanceCheckResponse(State.ACTIVE, performance)

        val TIMED_OUT = PerformanceCheckResponse(State.TIMED_OUT, null)
        val GONE = PerformanceCheckResponse(State.GONE, null)
    }
}
