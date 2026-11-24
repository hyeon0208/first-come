package io.github.hyeon0208.firstcome.show.api

import io.github.hyeon0208.firstcome.show.application.ShowReadService
import io.github.hyeon0208.firstcome.show.application.dto.SeatMapResponse
import io.github.hyeon0208.firstcome.show.application.dto.ShowResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/shows")
class ShowController(
    private val showReadService: ShowReadService,
) : ShowApi {
    @GetMapping
    override fun getShows(): List<ShowResponse> =
        showReadService.getShows()

    @GetMapping("/{showId}")
    override fun getShow(@PathVariable showId: Long): ShowResponse =
        showReadService.getShow(showId)

    @GetMapping("/{showId}/seat-map")
    override fun getSeatMap(@PathVariable showId: Long): SeatMapResponse =
        showReadService.getSeatMap(showId)
}
