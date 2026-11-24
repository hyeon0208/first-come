package io.github.hyeon0208.firstcome.show.application.dto

data class SeatMapResponse(
    val showId: Long,
    val venueName: String,
    val seats: List<SeatResponse>,
) {
    companion object {
        fun of(show: ShowResponse, seats: List<SeatResponse>): SeatMapResponse =
            SeatMapResponse(showId = show.id, venueName = show.venueName, seats = seats)
    }
}
