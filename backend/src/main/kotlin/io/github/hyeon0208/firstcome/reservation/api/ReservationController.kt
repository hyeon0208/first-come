package io.github.hyeon0208.firstcome.reservation.api

import io.github.hyeon0208.firstcome.reservation.application.ReservationFacade
import io.github.hyeon0208.firstcome.reservation.application.ReservationReadService
import io.github.hyeon0208.firstcome.reservation.application.dto.QuoteResponse
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.support.web.CurrentUserId
import io.github.hyeon0208.firstcome.support.web.PracticeRunHeader
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/reservations")
class ReservationController(
    private val reservationFacade: ReservationFacade,
    private val reservationReadService: ReservationReadService,
) : ReservationApi {
    @GetMapping("/quote")
    override fun getQuote(@CurrentUserId userId: Long, @RequestHeader(PracticeRunHeader.NAME) performanceId: Long): QuoteResponse =
        reservationReadService.getQuote(userId, performanceId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun reserve(
        @CurrentUserId userId: Long,
        @RequestHeader(PracticeRunHeader.NAME) performanceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
    ): ReservationResponse =
        reservationFacade.reserve(userId, performanceId, idempotencyKey)

    @GetMapping("/by-performance/{performanceId}")
    override fun getConfirmedReservation(@CurrentUserId userId: Long, @PathVariable performanceId: Long): ReservationResponse =
        reservationReadService.getConfirmedReservation(userId, performanceId)
}
