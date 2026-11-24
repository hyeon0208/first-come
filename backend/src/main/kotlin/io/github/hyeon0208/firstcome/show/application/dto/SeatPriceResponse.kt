package io.github.hyeon0208.firstcome.show.application.dto

import io.github.hyeon0208.firstcome.show.domain.ShowSeatPriceEntity
import io.github.hyeon0208.firstcome.show.domain.SeatGrade

data class SeatPriceResponse(
    val grade: SeatGrade,
    val price: Long,
) {
    companion object {
        fun from(seatPrice: ShowSeatPriceEntity): SeatPriceResponse =
            SeatPriceResponse(
                grade = seatPrice.grade,
                price = seatPrice.price,
            )
    }
}
