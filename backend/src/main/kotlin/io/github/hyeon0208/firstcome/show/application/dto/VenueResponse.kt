package io.github.hyeon0208.firstcome.show.application.dto

import io.github.hyeon0208.firstcome.show.domain.VenueEntity

data class VenueResponse(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(venue: VenueEntity): VenueResponse =
            VenueResponse(
                id = venue.id,
                name = venue.name,
            )
    }
}
