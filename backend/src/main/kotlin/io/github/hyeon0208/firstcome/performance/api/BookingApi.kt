package io.github.hyeon0208.firstcome.performance.api

import io.github.hyeon0208.firstcome.performance.application.dto.BookingStateResponse
import io.github.hyeon0208.firstcome.performance.application.dto.HoldResponse
import io.github.hyeon0208.firstcome.performance.application.dto.SeatStatusesResponse
import io.github.hyeon0208.firstcome.performance.application.dto.ValidatePricesRequest
import io.github.hyeon0208.firstcome.support.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

// 모든 요청은 X-Practice-Run 헤더의 회차가 그 사용자의 진행 중 회차여야 한다.
// 아니면 410 SESSION_EXPIRED, 제한 시간이 지나 끝났으면 410 TIME_OVER로 답한다.
@Tag(name = "Booking", description = "예매 화면: 입장, 좌석 가선점, 선택 완료(선점)")
interface BookingApi {
    @Operation(summary = "예매 화면 입장", description = "좌석 선택 시간 10분이 이때 시작된다. 다시 들어와도 시계는 처음 그대로다.")
    @ApiResponse(responseCode = "200", description = "입장")
    @ApiResponse(
        responseCode = "409",
        description = "NOT_OPEN_YET(예매 오픈 시각 전)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun enter(userId: Long, performanceId: Long): BookingStateResponse

    @Operation(summary = "내 예매 상태", description = "단계, 내 좌석, 타이머가 끝나는 시각과 서버 시각.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getState(userId: Long, performanceId: Long): BookingStateResponse

    @Operation(
        summary = "좌석 상태",
        description = "선택 불가 좌석과 내 좌석, 매진 여부. 화면은 들어올 때와 새로고침을 누를 때만 부른다. 매진이고 내 좌석이 없으면 회차가 매진으로 끝난다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getSeatStatuses(userId: Long, performanceId: Long): SeatStatusesResponse

    @Operation(
        summary = "좌석 가선점",
        description = "좌석을 누르는 순간 그 좌석을 묶는다. 첫 좌석부터 7분 동안 유지되고 좌석을 더 눌러도 늘지 않는다. 한 회차에 4석까지다.",
    )
    @ApiResponse(responseCode = "200", description = "가선점 성공")
    @ApiResponse(
        responseCode = "400",
        description = "SEAT_LIMIT_EXCEEDED(5번째 좌석)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "409",
        description = "SEAT_TAKEN(다른 참가자가 이미 가져감), SOLD_OUT(이미 가져간 좌석이고 남은 좌석도 없음. 회차가 매진으로 끝남), ALREADY_HOLDING",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun preselect(userId: Long, performanceId: Long, seatId: Long): BookingStateResponse

    @Operation(summary = "좌석 빼기")
    @ApiResponse(responseCode = "200", description = "가선점을 풂")
    fun unpreselect(userId: Long, performanceId: Long, seatId: Long): BookingStateResponse

    @Operation(summary = "전체삭제")
    @ApiResponse(responseCode = "200", description = "가선점을 모두 풂")
    fun unpreselectAll(userId: Long, performanceId: Long): BookingStateResponse

    @Operation(
        summary = "선택 완료",
        description = "가선점한 좌석 전체를 한 번에 선점한다. 예매 가능 시간 7분이 이때 시작된다. 못 잡은 좌석이 있으면 아무것도 선점하지 않고 그 목록을 돌려준다.",
    )
    @ApiResponse(responseCode = "200", description = "선점했거나(unselectableSeatIds가 빔), 못 잡은 좌석이 있어 선점하지 않음")
    @ApiResponse(
        responseCode = "400",
        description = "NO_SEATS_SELECTED",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun hold(userId: Long, performanceId: Long): HoldResponse

    @Operation(summary = "좌석 다시 고르기", description = "선점한 좌석을 모두 풀고 좌석 선택 단계로 돌아간다.")
    @ApiResponse(responseCode = "200", description = "선점을 풂")
    @ApiResponse(
        responseCode = "409",
        description = "BACK_BLOCKED(좌석 선택 시간 10분이 이미 끝남), SEATS_PAYING",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun releaseHold(userId: Long, performanceId: Long): BookingStateResponse

    @Operation(summary = "가격 선택 확인", description = "가격 종류별 수량이 선점한 좌석의 등급별 수와 같은지 확인한다.")
    @ApiResponse(responseCode = "204", description = "수량이 맞음")
    @ApiResponse(
        responseCode = "400",
        description = "PRICE_COUNT_MISMATCH",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun validatePrices(userId: Long, performanceId: Long, request: ValidatePricesRequest)
}
