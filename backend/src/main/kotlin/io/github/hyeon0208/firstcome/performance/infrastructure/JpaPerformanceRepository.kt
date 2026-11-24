package io.github.hyeon0208.firstcome.performance.infrastructure

import io.github.hyeon0208.firstcome.performance.domain.PerformanceEntity
import io.github.hyeon0208.firstcome.performance.domain.PerformanceStatus
import jakarta.persistence.LockModeType
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface JpaPerformanceRepository : JpaRepository<PerformanceEntity, Long> {
    fun findByActiveUserId(activeUserId: Long): PerformanceEntity?

    fun existsByActiveUserId(activeUserId: Long): Boolean

    fun countByStatus(status: PerformanceStatus): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PerformanceEntity p where p.id = :id")
    fun findByIdForUpdate(id: Long): PerformanceEntity?

    @Query("select p.id from PerformanceEntity p where p.status = :status order by p.id")
    fun findIdsByStatus(status: PerformanceStatus): List<Long>

    @Query("select p.id from PerformanceEntity p where p.status = ENDED and p.endedAt < :before order by p.id")
    fun findIdsEndedBefore(before: LocalDateTime): List<Long>

    @Query("select p.id from PerformanceEntity p where p.userId = :userId and p.status = ENDED order by p.id")
    fun findEndedIdsByUserId(userId: Long): List<Long>
}
