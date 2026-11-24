package io.github.hyeon0208.firstcome.show.api

import io.github.hyeon0208.firstcome.show.application.ShowReadService
import io.github.hyeon0208.firstcome.show.application.dto.ShowResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/venues/{venueId}/shows")
class ShowController(
    private val showReadService: ShowReadService,
) : ShowApi {
    @GetMapping
    override fun getShows(@PathVariable venueId: Long): List<ShowResponse> =
        showReadService.getShows(venueId)
}
