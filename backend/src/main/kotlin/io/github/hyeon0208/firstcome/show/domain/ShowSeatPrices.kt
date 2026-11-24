package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.show.domain.SeatGrade

class ShowSeatPrices(
    private val showId: Long,
    prices: List<ShowSeatPriceEntity>,
) {
    private val priceByGrade = prices.associate { it.grade to it.price }

    fun priceOf(grade: SeatGrade): Long =
        checkNotNull(priceByGrade[grade]) { "공연에 좌석 등급 가격이 없습니다. showId=$showId grade=$grade" }
}
