package io.github.hyeon0208.firstcome.reservation.application.dto

import io.github.hyeon0208.firstcome.show.application.dto.SeatResponse

data class ReservationSeatResponse(
    val seatId: Long,
    val name: String,
    val gradeName: String,
    val price: Long,
) {
    companion object {
        fun of(seat: SeatResponse, price: Long): ReservationSeatResponse =
            ReservationSeatResponse(seatId = seat.seatId, name = seat.name, gradeName = seat.gradeName, price = price)
    }
}
