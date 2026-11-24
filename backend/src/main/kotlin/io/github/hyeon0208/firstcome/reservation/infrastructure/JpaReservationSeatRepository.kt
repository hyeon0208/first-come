package io.github.hyeon0208.firstcome.reservation.infrastructure

import io.github.hyeon0208.firstcome.reservation.domain.ReservationSeatEntity
import org.springframework.data.jpa.repository.JpaRepository

interface JpaReservationSeatRepository : JpaRepository<ReservationSeatEntity, Long> {
    fun findAllByReservationId(reservationId: Long): List<ReservationSeatEntity>

    fun findAllByReservationIdIn(reservationIds: Collection<Long>): List<ReservationSeatEntity>
}
