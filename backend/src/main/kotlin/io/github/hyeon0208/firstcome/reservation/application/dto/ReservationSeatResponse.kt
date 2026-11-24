package io.github.hyeon0208.firstcome.reservation.application.dto

import io.github.hyeon0208.firstcome.reservation.domain.ReservationSeatEntity
import io.github.hyeon0208.firstcome.show.domain.SeatEntity
import io.github.hyeon0208.firstcome.show.domain.SeatGrade

data class ReservationSeatResponse(
    val performanceSeatId: Long,
    val row: Int,
    val column: Int,
    val grade: SeatGrade,
    val price: Long,
) {
    companion object {
        fun of(reservationSeat: ReservationSeatEntity, seat: SeatEntity): ReservationSeatResponse =
            ReservationSeatResponse(
                performanceSeatId = reservationSeat.performanceSeatId,
                row = seat.row,
                column = seat.column,
                grade = seat.grade,
                price = reservationSeat.price,
            )
    }
}
