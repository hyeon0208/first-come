package io.github.hyeon0208.firstcome.competitor.api

import io.github.hyeon0208.firstcome.competitor.application.dto.DifficultyResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Difficulty", description = "난이도(가상 경쟁자의 수와 속도)")
interface DifficultyApi {
    @Operation(
        summary = "난이도 목록",
        description = "난이도마다 가상 경쟁자 수, 들어오는 데 걸리는 시간, 매진까지 걸리는 대략의 시간을 돌려준다. 매진되지 않는 난이도는 soldOutSeconds가 null이다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getDifficulties(): List<DifficultyResponse>
}
