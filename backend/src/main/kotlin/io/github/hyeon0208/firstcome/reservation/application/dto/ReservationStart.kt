package io.github.hyeon0208.firstcome.reservation.application.dto

sealed interface ReservationStart {
    // 같은 멱등 키로 이미 확정된 예매가 있다
    data class AlreadyDone(val response: ReservationResponse) : ReservationStart

    data class Pending(val pending: PendingReservation) : ReservationStart
}
