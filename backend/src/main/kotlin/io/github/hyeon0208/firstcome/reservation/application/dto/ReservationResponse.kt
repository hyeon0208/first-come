package io.github.hyeon0208.firstcome.reservation.application.dto

import io.github.hyeon0208.firstcome.reservation.domain.ReservationEntity
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import java.time.LocalDateTime

data class ReservationResponse(
    val reservationId: Long,
    val reservationNo: String,
    val performanceId: Long,
    val status: ReservationStatus,
    val seats: List<ReservationSeatResponse>,
    val ticketAmount: Long,
    val feeAmount: Long,
    val amount: Long,
    val confirmedAt: LocalDateTime?,
) {
    companion object {
        fun of(reservation: ReservationEntity, seats: List<ReservationSeatResponse>): ReservationResponse =
            ReservationResponse(
                reservationId = reservation.id,
                reservationNo = reservation.reservationNo(),
                performanceId = reservation.performanceId,
                status = reservation.status,
                seats = seats,
                ticketAmount = reservation.ticketAmount,
                feeAmount = reservation.feeAmount,
                amount = reservation.amount(),
                confirmedAt = reservation.confirmedAt,
            )
    }
}
