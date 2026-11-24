package io.github.hyeon0208.firstcome.performance.api

import io.github.hyeon0208.firstcome.performance.application.PerformanceSeatReadService
import io.github.hyeon0208.firstcome.performance.application.PerformanceSeatWriteService
import io.github.hyeon0208.firstcome.performance.application.dto.HoldSeatsRequest
import io.github.hyeon0208.firstcome.performance.application.dto.HoldSeatsResponse
import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceSeatsResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/venues/{venueId}/shows/{showId}/performances/{performanceId}/seats")
class PerformanceSeatController(
    private val performanceSeatReadService: PerformanceSeatReadService,
    private val performanceSeatWriteService: PerformanceSeatWriteService,
) : PerformanceSeatApi {
    @GetMapping
    override fun getPerformanceSeats(
        @PathVariable venueId: Long,
        @PathVariable showId: Long,
        @PathVariable performanceId: Long,
    ): PerformanceSeatsResponse =
        performanceSeatReadService.getPerformanceSeats(venueId, showId, performanceId)

    @PostMapping("/hold")
    override fun holdSeats(
        @PathVariable venueId: Long,
        @PathVariable showId: Long,
        @PathVariable performanceId: Long,
        @RequestBody request: HoldSeatsRequest,
    ): HoldSeatsResponse =
        performanceSeatWriteService.holdSeats(venueId, showId, performanceId, request)
}
