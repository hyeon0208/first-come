package io.github.hyeon0208.firstcome.performance.application.dto

data class HoldResponse(
    val unselectableSeatIds: List<Long>,
    val state: BookingStateResponse,
) {
    companion object {
        fun of(held: HoldSeatsResponse, state: BookingStateResponse): HoldResponse =
            HoldResponse(unselectableSeatIds = held.unselectableSeatIds, state = state)
    }
}
