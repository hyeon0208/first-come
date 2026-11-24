package io.github.hyeon0208.firstcome.paymentmock

enum class FaultMode {
    NONE,

    // 결제를 거절한다
    DECLINE,

    // 정해진 시간만큼 늦게 승인한다
    DELAY,

    // 승인은 하지만 응답을 돌려주지 않는다
    NO_RESPONSE,
}
