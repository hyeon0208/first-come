package io.github.hyeon0208.firstcome.performance.domain

// 선택 가능 → 가선점 → 선점 → 결제 중 → 예매 완료
enum class PerformanceSeatStatus {
    AVAILABLE,
    PRESELECTED,
    HELD,
    PAYING,
    RESERVED,
}
