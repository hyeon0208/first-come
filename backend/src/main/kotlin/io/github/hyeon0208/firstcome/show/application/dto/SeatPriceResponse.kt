package io.github.hyeon0208.firstcome.show.application.dto

import io.github.hyeon0208.firstcome.show.SeatGrade
import io.github.hyeon0208.firstcome.show.domain.ShowSeatPriceEntity

data class SeatPriceResponse(
    val grade: SeatGrade,
    val name: String,
    val price: Long,
) {
    companion object {
        fun from(price: ShowSeatPriceEntity): SeatPriceResponse =
            SeatPriceResponse(grade = price.grade, name = price.name, price = price.price)
    }
}
