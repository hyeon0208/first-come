package io.github.hyeon0208.firstcome.show.application

import io.github.hyeon0208.firstcome.show.application.dto.VenueResponse
import io.github.hyeon0208.firstcome.show.infrastructure.JpaVenueRepository
import org.springframework.stereotype.Service

@Service
class VenueReadService(
    private val venueRepository: JpaVenueRepository,
) {
    fun getVenues(): List<VenueResponse> =
        venueRepository.findAll().map(VenueResponse::from)
}
