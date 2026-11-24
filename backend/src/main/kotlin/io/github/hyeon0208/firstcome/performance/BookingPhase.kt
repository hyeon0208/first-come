package io.github.hyeon0208.firstcome.performance

// 한 참가자가 좌석을 가져가는 단계.
enum class BookingPhase {
    // 좌석을 고르는 중(가선점한 좌석이 없을 수도 있다)
    SELECTING,

    // 선택 완료로 좌석을 선점했다
    HELD,

    // 선점한 좌석의 결제가 진행 중이다
    PAYING,

    // 예매가 확정됐다
    RESERVED,
}
