package io.github.hyeon0208.firstcome.show.api

import io.github.hyeon0208.firstcome.show.application.dto.ShowResponse
import io.github.hyeon0208.firstcome.support.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Show", description = "공연·회차")
interface ShowApi {
    @Operation(
        summary = "공연 목록",
        description = "공연장의 공연을 등급별 가격, 회차(시작 시각 순)와 함께 돌려준다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "404",
        description = "VENUE_NOT_FOUND: 없는 공연장",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getShows(venueId: Long): List<ShowResponse>
}
