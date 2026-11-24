package io.github.hyeon0208.firstcome.reservation.api

import io.github.hyeon0208.firstcome.reservation.application.ReservationFacade
import io.github.hyeon0208.firstcome.reservation.application.ReservationReadService
import io.github.hyeon0208.firstcome.reservation.application.dto.CreateReservationRequest
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ReservationController(
    private val reservationFacade: ReservationFacade,
    private val reservationReadService: ReservationReadService,
) : ReservationApi {

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    override fun createReservation(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: CreateReservationRequest,
    ): ReservationResponse =
        reservationFacade.createReservation(request, idempotencyKey)

    @GetMapping("/users/{userId}/reservations")
    override fun getUserReservations(@PathVariable userId: Long): List<ReservationResponse> =
        reservationReadService.getUserReservations(userId)
}
