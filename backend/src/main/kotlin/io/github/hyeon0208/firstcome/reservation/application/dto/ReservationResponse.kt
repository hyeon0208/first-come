package io.github.hyeon0208.firstcome.reservation.application.dto

import io.github.hyeon0208.firstcome.performance.domain.PerformanceEntity
import io.github.hyeon0208.firstcome.reservation.domain.ReservationEntity
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import io.github.hyeon0208.firstcome.show.domain.ShowEntity
import java.time.LocalDateTime

data class ReservationResponse(
    val reservationId: Long,
    val status: ReservationStatus,
    val performanceId: Long,
    val showTitle: String,
    val startAt: LocalDateTime,
    val seats: List<ReservationSeatResponse>,
    val amount: Long,
    val confirmedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            reservation: ReservationEntity,
            show: ShowEntity,
            performance: PerformanceEntity,
            seats: List<ReservationSeatResponse>,
        ): ReservationResponse =
            ReservationResponse(
                reservationId = reservation.id,
                status = reservation.status,
                performanceId = performance.id,
                showTitle = show.title,
                startAt = performance.startAt,
                seats = seats,
                amount = reservation.amount,
                confirmedAt = reservation.confirmedAt,
            )
    }
}
