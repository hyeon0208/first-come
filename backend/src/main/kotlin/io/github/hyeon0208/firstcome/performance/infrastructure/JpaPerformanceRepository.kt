package io.github.hyeon0208.firstcome.performance.infrastructure

import io.github.hyeon0208.firstcome.performance.domain.PerformanceEntity
import io.github.hyeon0208.firstcome.performance.domain.PerformanceStatus
import jakarta.persistence.LockModeType
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface JpaPerformanceRepository : JpaRepository<PerformanceEntity, Long> {
    fun existsByActiveUserId(activeUserId: Long): Boolean

    fun countByStatus(status: PerformanceStatus): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PerformanceEntity p where p.id = :id")
    fun findByIdForUpdate(id: Long): PerformanceEntity?

    // 회차가 끝나거나 지워지는 것만 막고, 같은 회차의 다른 참가자들은 동시에 읽을 수 있게 공유 잠금으로 읽는다.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from PerformanceEntity p where p.id = :id")
    fun findByIdForShare(id: Long): PerformanceEntity?

    // 잠그지 않고 먼저 읽으면 그 뒤의 잠금 조회가 옛 상태를 돌려줄 수 있어, 한 번에 잠가 읽는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PerformanceEntity p where p.activeUserId = :userId")
    fun findByActiveUserIdForUpdate(userId: Long): PerformanceEntity?

    @Query("select p.id from PerformanceEntity p where p.status = :status order by p.id")
    fun findIdsByStatus(status: PerformanceStatus): List<Long>

    @Query("select p.id from PerformanceEntity p where p.status = ENDED and p.endedAt < :before order by p.id")
    fun findIdsEndedBefore(before: LocalDateTime): List<Long>

    @Query("select p.id from PerformanceEntity p where p.userId = :userId and p.status = ENDED order by p.id")
    fun findEndedIdsByUserId(userId: Long): List<Long>
}
