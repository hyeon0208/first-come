package io.github.hyeon0208.firstcome.reservation.infrastructure

import io.github.hyeon0208.firstcome.reservation.domain.ReservationSeatEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface JpaReservationSeatRepository : JpaRepository<ReservationSeatEntity, Long> {
    fun findAllByReservationId(reservationId: Long): List<ReservationSeatEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ReservationSeatEntity s where s.performanceId = :performanceId")
    fun deleteAllByPerformanceId(performanceId: Long): Int
}
