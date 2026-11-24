package io.github.hyeon0208.firstcome.record.application

import io.github.hyeon0208.firstcome.record.application.dto.RecordResponse
import io.github.hyeon0208.firstcome.record.domain.RecordErrorCode
import io.github.hyeon0208.firstcome.record.infrastructure.JpaRecordRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import org.springframework.stereotype.Service

@Service
class RecordReadService(
    private val recordRepository: JpaRecordRepository,
) {
    fun getRecords(userId: Long): List<RecordResponse> =
        recordRepository.findAllByUserIdOrderByIdDesc(userId).map(RecordResponse::from)

    fun getRecord(userId: Long, performanceId: Long): RecordResponse =
        recordRepository.findByUserIdAndPerformanceId(userId, performanceId)?.let(RecordResponse::from)
            ?: throw BusinessException(RecordErrorCode.RECORD_NOT_FOUND)
}
