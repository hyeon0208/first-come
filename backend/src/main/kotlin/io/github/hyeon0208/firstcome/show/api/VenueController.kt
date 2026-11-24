package io.github.hyeon0208.firstcome.show.api

import io.github.hyeon0208.firstcome.show.application.VenueReadService
import io.github.hyeon0208.firstcome.show.application.dto.VenueResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/venues")
class VenueController(
    private val venueReadService: VenueReadService,
) : VenueApi {
    @GetMapping
    override fun getVenues(): List<VenueResponse> =
        venueReadService.getVenues()
}
