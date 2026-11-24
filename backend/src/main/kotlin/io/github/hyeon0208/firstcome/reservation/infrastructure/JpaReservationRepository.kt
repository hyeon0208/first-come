package io.github.hyeon0208.firstcome.reservation.infrastructure

import io.github.hyeon0208.firstcome.reservation.domain.ReservationEntity
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface JpaReservationRepository : JpaRepository<ReservationEntity, Long> {
    fun findByPerformanceIdAndParticipantNoAndIdempotencyKey(performanceId: Long, participantNo: Int, idempotencyKey: String): ReservationEntity?

    fun findByPerformanceIdAndParticipantNoAndStatus(performanceId: Long, participantNo: Int, status: ReservationStatus): ReservationEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservationEntity r where r.id = :id")
    fun findByIdForUpdate(id: Long): ReservationEntity?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ReservationEntity r where r.performanceId = :performanceId")
    fun deleteAllByPerformanceId(performanceId: Long): Int
}
