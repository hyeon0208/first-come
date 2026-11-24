package io.github.hyeon0208.firstcome.show.application.dto

import io.github.hyeon0208.firstcome.performance.domain.PerformanceEntity
import io.github.hyeon0208.firstcome.show.domain.ShowEntity
import io.github.hyeon0208.firstcome.show.domain.ShowSeatPriceEntity

data class ShowResponse(
    val id: Long,
    val title: String,
    val seatPrices: List<SeatPriceResponse>,
    val performances: List<PerformanceResponse>,
) {
    companion object {
        fun of(
            show: ShowEntity,
            seatPrices: List<ShowSeatPriceEntity>,
            performances: List<PerformanceEntity>,
        ): ShowResponse =
            ShowResponse(
                id = show.id,
                title = show.title,
                seatPrices = seatPrices.sortedBy { it.grade }.map(SeatPriceResponse::from),
                performances = performances
                    .sortedBy { it.startAt }
                    .map(PerformanceResponse::from),
            )
    }
}
