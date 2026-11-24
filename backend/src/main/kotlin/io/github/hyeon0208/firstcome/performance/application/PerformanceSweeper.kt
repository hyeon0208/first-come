package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.domain.PerformanceStatus
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceRepository
import java.time.Clock
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// 요청이 오지 않아도 해야 하는 정리: 방치된 회차를 끝내고, 끝난 지 오래된 회차의 데이터를 지운다.
// 좌석의 만료는 여기서 풀지 않는다. 만료 시각과 현재 시각을 비교해 판정하기 때문이다.
@Component
@ConditionalOnProperty("booking.sweep-enabled", havingValue = "true")
class PerformanceSweeper(
    private val performanceRepository: JpaPerformanceRepository,
    private val performanceWriteService: PerformanceWriteService,
    private val bookingProperties: BookingProperties,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun endInterruptedPerformances() {
        val count = performanceWriteService.endAllActive()
        if (count > 0) {
            logger.warn("서버가 다시 떠서 진행 중이던 회차를 중단으로 끝냄 count={}", count)
        }
    }

    @Scheduled(fixedDelayString = "\${booking.sweep-interval}")
    fun sweep() {
        performanceRepository.findIdsByStatus(PerformanceStatus.ACTIVE).forEach { each(it, performanceWriteService::endIfDue) }
        val purgeBefore = LocalDateTime.now(clock).minus(bookingProperties.purgeDelay)
        performanceRepository.findIdsEndedBefore(purgeBefore).forEach { each(it, performanceWriteService::purge) }
    }

    // 한 회차에서 실패해도 나머지는 계속 처리한다.
    private fun each(performanceId: Long, action: (Long) -> Unit) {
        try {
            action(performanceId)
        } catch (exception: Exception) {
            logger.error("회차 정리 실패 performanceId={}", performanceId, exception)
        }
    }
}
