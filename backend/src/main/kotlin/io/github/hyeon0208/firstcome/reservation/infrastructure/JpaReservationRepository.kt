package io.github.hyeon0208.firstcome.reservation.infrastructure

import io.github.hyeon0208.firstcome.reservation.domain.ReservationEntity
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface JpaReservationRepository : JpaRepository<ReservationEntity, Long> {
    fun findByUserIdAndIdempotencyKey(userId: Long, idempotencyKey: String): ReservationEntity?

    fun findAllByUserIdAndStatusOrderByIdDesc(userId: Long, status: ReservationStatus): List<ReservationEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservationEntity r where r.id = :id")
    fun findByIdForUpdate(id: Long): ReservationEntity?
}
