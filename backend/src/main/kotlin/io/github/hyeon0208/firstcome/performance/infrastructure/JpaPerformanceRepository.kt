package io.github.hyeon0208.firstcome.performance.infrastructure

import io.github.hyeon0208.firstcome.performance.domain.PerformanceEntity
import org.springframework.data.jpa.repository.JpaRepository

interface JpaPerformanceRepository : JpaRepository<PerformanceEntity, Long> {
    fun findAllByShowIdInOrderByStartAt(showIds: Collection<Long>): List<PerformanceEntity>

    fun findByIdAndShowId(id: Long, showId: Long): PerformanceEntity?
}
