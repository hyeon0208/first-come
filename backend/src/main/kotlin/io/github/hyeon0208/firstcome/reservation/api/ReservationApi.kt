package io.github.hyeon0208.firstcome.reservation.api

import io.github.hyeon0208.firstcome.reservation.application.dto.CreateReservationRequest
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.support.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size

@Tag(name = "Reservation", description = "예약")
interface ReservationApi {
    @Operation(
        summary = "예약 확정(결제)",
        description = "임시 배정한 좌석 전체를 결제하고 예약을 확정한다. 요청 좌석은 지금 유효한 내 임시 배정 전체와 같아야 한다(부분 예약 불가).",
    )
    @ApiResponse(responseCode = "201", description = "예약 확정. 같은 멱등 키로 다시 보내면 결제 없이 그 예약을 돌려준다(확정된 예약).")
    @ApiResponse(
        responseCode = "400",
        description = "INVALID_REQUEST(Idempotency-Key 누락/길이), SEAT_COUNT_OUT_OF_RANGE, DUPLICATED_SEAT",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "402",
        description = "PAYMENT_DECLINED(결제 거절. 좌석은 임시 배정 그대로다)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "404",
        description = "USER_NOT_FOUND, PERFORMANCE_NOT_FOUND",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "409",
        description = "HOLD_NOT_FOUND, HOLD_EXPIRED, HOLD_MISMATCH(부분 예약), HOLD_LOST(결제 중 좌석을 잃어 결제를 취소함), PERFORMANCE_ALREADY_STARTED, " +
            "IDEMPOTENCY_KEY_IN_PROGRESS(같은 멱등 키 처리 중), IDEMPOTENCY_KEY_ALREADY_FAILED(실패로 끝난 멱등 키)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "422",
        description = "IDEMPOTENCY_KEY_MISMATCH(같은 멱등 키로 다른 회차, 좌석)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun createReservation(
        @Parameter(description = "결제 요청마다 클라이언트가 만든 멱등 키(1~64자). 같은 요청을 다시 보낼 때는 같은 멱등 키를 쓴다.", required = true)
        @Size(min = 1, max = 64)
        idempotencyKey: String,
        request: CreateReservationRequest,
    ): ReservationResponse

    @Operation(
        summary = "내 예약 조회",
        description = "확정된 내 예약을 최신순으로 돌려준다. 결제 대기와 실패로 끝난 예약은 보이지 않는다. 페이지 나눔은 없다.",
    )
    @ApiResponse(responseCode = "200", description = "내 예약 목록(없으면 빈 목록)")
    @ApiResponse(
        responseCode = "404",
        description = "USER_NOT_FOUND",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getUserReservations(userId: Long): List<ReservationResponse>
}
