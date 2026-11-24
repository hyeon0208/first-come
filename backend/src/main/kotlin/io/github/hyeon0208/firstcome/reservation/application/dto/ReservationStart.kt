package io.github.hyeon0208.firstcome.reservation.application.dto

sealed interface ReservationStart {
    data class AlreadyDone(val response: ReservationResponse) : ReservationStart

    data class Pending(val pending: PendingReservation) : ReservationStart
}
