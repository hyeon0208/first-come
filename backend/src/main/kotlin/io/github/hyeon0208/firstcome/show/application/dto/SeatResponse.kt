package io.github.hyeon0208.firstcome.show.application.dto

import io.github.hyeon0208.firstcome.show.SeatGrade
import io.github.hyeon0208.firstcome.show.domain.SeatEntity
import io.github.hyeon0208.firstcome.show.domain.ShowSeatPriceEntity

// 한 공연에서 좌석 하나의 위치와 등급, 가격.
data class SeatResponse(
    val seatId: Long,
    val floor: Int,
    val block: String,
    val rowNo: Int,
    val seatNo: Int,
    val name: String,
    val grade: SeatGrade,
    val gradeName: String,
    val price: Long,
) {
    companion object {
        fun of(seat: SeatEntity, price: ShowSeatPriceEntity): SeatResponse =
            SeatResponse(
                seatId = seat.id,
                floor = seat.floor,
                block = seat.block,
                rowNo = seat.rowNo,
                seatNo = seat.seatNo,
                name = seat.displayName,
                grade = seat.grade,
                gradeName = price.name,
                price = price.price,
            )
    }
}
