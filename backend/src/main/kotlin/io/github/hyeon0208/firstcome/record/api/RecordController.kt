package io.github.hyeon0208.firstcome.record.api

import io.github.hyeon0208.firstcome.record.application.RecordReadService
import io.github.hyeon0208.firstcome.record.application.dto.RecordResponse
import io.github.hyeon0208.firstcome.support.web.CurrentUserId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/records")
class RecordController(
    private val recordReadService: RecordReadService,
) : RecordApi {
    @GetMapping
    override fun getRecords(@CurrentUserId userId: Long): List<RecordResponse> =
        recordReadService.getRecords(userId)

    @GetMapping("/by-performance/{performanceId}")
    override fun getRecord(@CurrentUserId userId: Long, @PathVariable performanceId: Long): RecordResponse =
        recordReadService.getRecord(userId, performanceId)
}
