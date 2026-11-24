package io.github.hyeon0208.firstcome.performance.infrastructure

import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatEntity
import jakarta.persistence.LockModeType
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface JpaPerformanceSeatRepository : JpaRepository<PerformanceSeatEntity, Long> {
    fun findAllByPerformanceIdOrderById(performanceId: Long): List<PerformanceSeatEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ps from PerformanceSeatEntity ps where ps.id in :ids order by ps.id")
    fun findAllByIdInOrderByIdForUpdate(ids: Collection<Long>): List<PerformanceSeatEntity>

    @Query(
        """
        select ps from PerformanceSeatEntity ps
        where ps.holderId = :holderId and ps.performanceId = :performanceId and ps.status = HELD
        """,
    )
    fun findHeldSeats(performanceId: Long, holderId: Long): List<PerformanceSeatEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity ps
        set ps.status = RESERVED, ps.holderId = null, ps.holdExpiresAt = null, ps.updatedAt = :now
        where ps.id in :ids and ps.holderId = :holderId and ps.status = HELD
        """,
    )
    fun reserveSeats(ids: Collection<Long>, holderId: Long, now: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity ps
        set ps.status = AVAILABLE, ps.holderId = null, ps.holdExpiresAt = null, ps.updatedAt = :now
        where ps.holderId = :holderId and ps.performanceId = :performanceId and ps.status = HELD
        """,
    )
    fun releaseHolds(performanceId: Long, holderId: Long, now: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity ps
        set ps.status = HELD, ps.holderId = :holderId, ps.holdExpiresAt = :holdExpiresAt, ps.updatedAt = :now
        where ps.id in :ids and ps.performanceId = :performanceId
          and (ps.status = AVAILABLE or (ps.status = HELD and ps.holdExpiresAt < :now))
        """,
    )
    fun holdSeats(
        ids: Collection<Long>,
        performanceId: Long,
        holderId: Long,
        holdExpiresAt: LocalDateTime,
        now: LocalDateTime,
    ): Int
}
