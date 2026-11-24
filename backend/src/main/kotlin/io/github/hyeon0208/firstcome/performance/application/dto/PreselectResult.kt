package io.github.hyeon0208.firstcome.performance.application.dto

// 좌석을 눌렀을 때의 결과. 거절도 흔한 결과라 예외로 던지지 않는다.
enum class PreselectResult {
    SELECTED,

    // 다른 참가자가 이미 가져갔다
    TAKEN,

    // 이미 가져간 좌석이고, 남은 좌석도 하나도 없다
    SOLD_OUT,
}
