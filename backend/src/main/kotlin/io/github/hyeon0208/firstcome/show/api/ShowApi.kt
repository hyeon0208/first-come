package io.github.hyeon0208.firstcome.show.api

import io.github.hyeon0208.firstcome.show.application.dto.SeatMapResponse
import io.github.hyeon0208.firstcome.show.application.dto.ShowResponse
import io.github.hyeon0208.firstcome.support.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Show", description = "공연")
interface ShowApi {
    @Operation(summary = "공연 목록", description = "연습할 수 있는 공연을 등급별 가격과 함께 돌려준다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getShows(): List<ShowResponse>

    @Operation(summary = "공연 상세")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "404",
        description = "SHOW_NOT_FOUND",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getShow(showId: Long): ShowResponse

    @Operation(
        summary = "좌석 배치",
        description = "공연장의 모든 좌석을 위치(층, 구역, 열, 번호), 등급, 가격과 함께 돌려준다. 판매 상태는 담지 않으며 바뀌지 않는 데이터다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "404",
        description = "SHOW_NOT_FOUND",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getSeatMap(showId: Long): SeatMapResponse
}
