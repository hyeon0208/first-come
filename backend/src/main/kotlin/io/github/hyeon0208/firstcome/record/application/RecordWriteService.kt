package io.github.hyeon0208.firstcome.record.application

import io.github.hyeon0208.firstcome.performance.PerformanceEnded
import io.github.hyeon0208.firstcome.record.domain.RecordEntity
import io.github.hyeon0208.firstcome.record.infrastructure.JpaRecordRepository
import io.github.hyeon0208.firstcome.show.application.ShowReadService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RecordWriteService(
    private val recordRepository: JpaRecordRepository,
    private val showReadService: ShowReadService,
) {
    // 회차가 끝나면 결과만 남긴다. 회차를 끝내는 트랜잭션 안에서 함께 저장된다.
    @EventListener
    fun createRecord(event: PerformanceEnded) {
        recordRepository.save(
            RecordEntity(
                userId = event.userId,
                performanceId = event.performanceId,
                showTitle = showReadService.getShow(event.showId).title,
                difficulty = event.difficulty,
                result = event.result,
                elapsedSeconds = event.elapsedSeconds,
                endedAt = event.endedAt,
            ),
        )
    }
}
