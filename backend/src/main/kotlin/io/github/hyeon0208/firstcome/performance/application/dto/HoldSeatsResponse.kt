package io.github.hyeon0208.firstcome.performance.application.dto

// 선택 완료의 결과. 못 잡은 좌석이 하나라도 있으면 아무것도 선점하지 않고 그 목록을 돌려준다.
data class HoldSeatsResponse(
    val heldSeatIds: List<Long>,
    val unselectableSeatIds: List<Long>,
) {
    val isHeld: Boolean
        get() = unselectableSeatIds.isEmpty()
}
