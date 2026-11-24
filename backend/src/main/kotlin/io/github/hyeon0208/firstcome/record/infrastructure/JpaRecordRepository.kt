package io.github.hyeon0208.firstcome.record.infrastructure

import io.github.hyeon0208.firstcome.record.domain.RecordEntity
import org.springframework.data.jpa.repository.JpaRepository

interface JpaRecordRepository : JpaRepository<RecordEntity, Long> {
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<RecordEntity>

    fun findByUserIdAndPerformanceId(userId: Long, performanceId: Long): RecordEntity?
}
