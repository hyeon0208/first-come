package io.github.hyeon0208.firstcome.record.api

import io.github.hyeon0208.firstcome.record.application.dto.RecordResponse
import io.github.hyeon0208.firstcome.support.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Record", description = "연습 기록(내 예매)")
interface RecordApi {
    @Operation(summary = "내 연습 기록", description = "끝난 회차의 결과(성공, 매진, 시간 초과, 중단)와 걸린 시간을 최근 것부터 돌려준다.")
    @ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 목록)")
    fun getRecords(userId: Long): List<RecordResponse>

    @Operation(summary = "한 회차의 연습 기록")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "404",
        description = "RECORD_NOT_FOUND(없거나, 남의 회차이거나, 아직 끝나지 않음)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getRecord(userId: Long, performanceId: Long): RecordResponse
}
