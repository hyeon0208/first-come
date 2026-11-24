package io.github.hyeon0208.firstcome.performance.domain

enum class PerformanceSeatStatus {
    AVAILABLE,
    HELD,
    PAYING,
    RESERVED,
    ;

    companion object {
        // 한 사용자가 쥐고 있는 상태. 이 상태일 때만 임시 배정 사용자와 만료 시각이 있다.
        val OWNED = setOf(HELD, PAYING)
    }
}
