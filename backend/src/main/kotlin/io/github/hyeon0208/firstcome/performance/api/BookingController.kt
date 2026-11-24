package io.github.hyeon0208.firstcome.performance.api

import io.github.hyeon0208.firstcome.performance.application.BookingFacade
import io.github.hyeon0208.firstcome.performance.application.dto.BookingStateResponse
import io.github.hyeon0208.firstcome.performance.application.dto.HoldResponse
import io.github.hyeon0208.firstcome.performance.application.dto.SeatStatusesResponse
import io.github.hyeon0208.firstcome.performance.application.dto.ValidatePricesRequest
import io.github.hyeon0208.firstcome.support.web.CurrentUserId
import io.github.hyeon0208.firstcome.support.web.PracticeRunHeader
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/booking")
class BookingController(
    private val bookingFacade: BookingFacade,
) : BookingApi {
    @PostMapping("/enter")
    override fun enter(@CurrentUserId userId: Long, @RequestHeader(PracticeRunHeader.NAME) performanceId: Long): BookingStateResponse =
        bookingFacade.enter(userId, performanceId)

    @GetMapping("/state")
    override fun getState(@CurrentUserId userId: Long, @RequestHeader(PracticeRunHeader.NAME) performanceId: Long): BookingStateResponse =
        bookingFacade.getState(userId, performanceId)

    @GetMapping("/seats")
    override fun getSeatStatuses(@CurrentUserId userId: Long, @RequestHeader(PracticeRunHeader.NAME) performanceId: Long): SeatStatusesResponse =
        bookingFacade.getSeatStatuses(userId, performanceId)

    @PostMapping("/seats/{seatId}/preselect")
    override fun preselect(
        @CurrentUserId userId: Long,
        @RequestHeader(PracticeRunHeader.NAME) performanceId: Long,
        @PathVariable seatId: Long,
    ): BookingStateResponse =
        bookingFacade.preselect(userId, performanceId, seatId)

    @DeleteMapping("/seats/{seatId}/preselect")
    override fun unpreselect(
        @CurrentUserId userId: Long,
        @RequestHeader(PracticeRunHeader.NAME) performanceId: Long,
        @PathVariable seatId: Long,
    ): BookingStateResponse =
        bookingFacade.unpreselect(userId, performanceId, seatId)

    @DeleteMapping("/preselects")
    override fun unpreselectAll(@CurrentUserId userId: Long, @RequestHeader(PracticeRunHeader.NAME) performanceId: Long): BookingStateResponse =
        bookingFacade.unpreselectAll(userId, performanceId)

    @PostMapping("/hold")
    override fun hold(@CurrentUserId userId: Long, @RequestHeader(PracticeRunHeader.NAME) performanceId: Long): HoldResponse =
        bookingFacade.hold(userId, performanceId)

    @DeleteMapping("/hold")
    override fun releaseHold(@CurrentUserId userId: Long, @RequestHeader(PracticeRunHeader.NAME) performanceId: Long): BookingStateResponse =
        bookingFacade.releaseHold(userId, performanceId)

    @PostMapping("/prices/validate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun validatePrices(
        @CurrentUserId userId: Long,
        @RequestHeader(PracticeRunHeader.NAME) performanceId: Long,
        @RequestBody request: ValidatePricesRequest,
    ) {
        bookingFacade.validatePrices(userId, performanceId, request)
    }
}
