package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceResponse
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceRepository
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class PerformanceReadService(
    private val performanceRepository: JpaPerformanceRepository,
    private val clock: Clock,
) {
    // 끝났더라도 아직 지워지지 않았으면 돌려준다. 그 사용자의 회차가 아니면 없는 것으로 본다.
    fun findPerformance(userId: Long, performanceId: Long): PerformanceResponse? =
        performanceRepository.findByIdOrNull(performanceId)
            ?.takeIf { it.userId == userId }
            ?.let { PerformanceResponse.of(it, LocalDateTime.now(clock)) }

    fun findPerformance(performanceId: Long): PerformanceResponse? =
        performanceRepository.findByIdOrNull(performanceId)?.let { PerformanceResponse.of(it, LocalDateTime.now(clock)) }

    fun isStarted(performanceId: Long): Boolean =
        performanceRepository.findByIdOrNull(performanceId)?.isStartedAt(LocalDateTime.now(clock)) ?: true
}
