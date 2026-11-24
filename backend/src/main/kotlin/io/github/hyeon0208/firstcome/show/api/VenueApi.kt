package io.github.hyeon0208.firstcome.show.api

import io.github.hyeon0208.firstcome.show.application.dto.VenueResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Venue", description = "공연장")
interface VenueApi {
    @Operation(summary = "공연장 목록", description = "공연장 id와 이름을 id 순으로 돌려준다. 공연 목록 조회의 시작점이다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getVenues(): List<VenueResponse>
}
