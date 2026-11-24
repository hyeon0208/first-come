package io.github.hyeon0208.firstcome.performance.api

import io.github.hyeon0208.firstcome.performance.application.dto.CreatePerformanceRequest
import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceResponse
import io.github.hyeon0208.firstcome.support.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "Performance", description = "회차(연습 한 번)")
interface PerformanceApi {
    @Operation(
        summary = "연습 시작",
        description = "그 사용자만의 회차를 만든다. 예매 오픈 시각은 10초 뒤이고 가상 경쟁자도 그때부터 움직인다. 이전에 끝난 회차의 좌석과 예매 데이터는 지워진다.",
    )
    @ApiResponse(responseCode = "201", description = "회차를 만듦")
    @ApiResponse(
        responseCode = "409",
        description = "ALREADY_IN_PROGRESS(진행 중인 회차가 있음)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "503",
        description = "PRACTICE_FULL(동시에 진행되는 회차 수가 상한에 닿음)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun createPerformance(userId: Long, request: CreatePerformanceRequest): PerformanceResponse

    @Operation(summary = "진행 중인 회차", description = "진행 중인 회차가 없으면 본문 없이 204로 답한다.")
    @ApiResponse(responseCode = "200", description = "진행 중인 회차")
    @ApiResponse(responseCode = "204", description = "진행 중인 회차가 없음")
    fun getCurrentPerformance(userId: Long): ResponseEntity<PerformanceResponse>

    @Operation(summary = "처음부터 다시", description = "진행 중인 회차를 중단으로 끝낸다. 진행 중인 회차가 없어도 성공으로 답한다.")
    @ApiResponse(responseCode = "204", description = "중단으로 끝냄")
    @ApiResponse(
        responseCode = "409",
        description = "SEATS_PAYING(결제가 진행 중이라 끝낼 수 없음)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun abandonCurrentPerformance(userId: Long)
}
