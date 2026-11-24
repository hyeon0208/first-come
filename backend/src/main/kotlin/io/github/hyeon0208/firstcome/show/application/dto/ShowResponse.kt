package io.github.hyeon0208.firstcome.show.application.dto

import io.github.hyeon0208.firstcome.show.domain.ShowEntity
import io.github.hyeon0208.firstcome.show.domain.ShowSeatPriceEntity
import io.github.hyeon0208.firstcome.show.domain.VenueEntity

data class ShowResponse(
    val id: Long,
    val title: String,
    val venueName: String,
    val runningMinutes: Int,
    val ageLimit: String,
    val seatPrices: List<SeatPriceResponse>,
) {
    companion object {
        fun of(show: ShowEntity, venue: VenueEntity, prices: List<ShowSeatPriceEntity>): ShowResponse =
            ShowResponse(
                id = show.id,
                title = show.title,
                venueName = venue.name,
                runningMinutes = show.runningMinutes,
                ageLimit = show.ageLimit,
                seatPrices = prices.map(SeatPriceResponse::from),
            )
    }
}
