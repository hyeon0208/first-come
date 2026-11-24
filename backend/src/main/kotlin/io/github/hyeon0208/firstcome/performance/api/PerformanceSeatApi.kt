package io.github.hyeon0208.firstcome.performance.api

import io.github.hyeon0208.firstcome.performance.application.dto.HoldSeatsRequest
import io.github.hyeon0208.firstcome.performance.application.dto.HoldSeatsResponse
import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceSeatsResponse
import io.github.hyeon0208.firstcome.support.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "PerformanceSeat", description = "공연 회차별 좌석")
interface PerformanceSeatApi {
    @Operation(
        summary = "회차 좌석 조회",
        description = "회차의 전체 좌석을 행·열 순서로 돌려준다. 좌석마다 등급, 가격, 상태(AVAILABLE 예약 가능,HELD 임시 배정,RESERVED 예약 완료)를 담는다. 임시 배정 만료 시각이 지난 좌석은 AVAILABLE이다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "404",
        description = "PERFORMANCE_NOT_FOUND: 없는 회차이거나 경로의 공연장/공연과 맞지 않음",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getPerformanceSeats(venueId: Long, showId: Long, performanceId: Long): PerformanceSeatsResponse

    @Operation(
        summary = "좌석 임시 배정",
        description = "고른 좌석(1~5석)을 5분간 그 사용자에게 임시 배정한다. 같은 회차의 기존 임시 배정은 새 요청으로 대체되고, 하나라도 배정하지 못하면 기존 배정은 그대로 남는다. 같은 좌석을 여러 명이 동시에 요청하면 한 명만 성공한다.",
    )
    @ApiResponse(responseCode = "200", description = "임시 배정 성공")
    @ApiResponse(
        responseCode = "400",
        description = "SEAT_COUNT_OUT_OF_RANGE , DUPLICATED_SEAT , INVALID_REQUEST",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "404",
        description = "USER_NOT_FOUND , PERFORMANCE_NOT_FOUND , SEAT_NOT_FOUND(이 회차에 없는 좌석)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "409",
        description = "SEAT_NOT_AVAILABLE(이미 임시 배정/예약된 좌석) · PERFORMANCE_ALREADY_STARTED · LOCK_CONFLICT(다시 시도)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun holdSeats(venueId: Long, showId: Long, performanceId: Long, request: HoldSeatsRequest): HoldSeatsResponse
}
