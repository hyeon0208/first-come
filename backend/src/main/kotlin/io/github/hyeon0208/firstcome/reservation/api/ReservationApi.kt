package io.github.hyeon0208.firstcome.reservation.api

import io.github.hyeon0208.firstcome.reservation.application.dto.QuoteResponse
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.support.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size

@Tag(name = "Reservation", description = "예매와 결제")
interface ReservationApi {
    @Operation(summary = "결제할 금액", description = "선점한 좌석의 가격과 예매수수료(장당)를 더한 금액을 돌려준다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "409",
        description = "HOLD_NOT_FOUND(선점한 좌석이 없음)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getQuote(userId: Long, performanceId: Long): QuoteResponse

    @Operation(
        summary = "결제하고 예매 확정",
        description = "선점한 좌석 전체를 결제한다. 결제가 승인된 뒤 예매 가능 시간과 공연 시작 시각을 다시 확인하고, 어긋나면 확정하지 않고 결제를 자동으로 취소한다. " +
            "같은 선점에는 결제가 한 번에 한 건만 진행되며, 진행 중에 온 요청은 멱등 키가 달라도 결제를 부르지 않고 거절한다.",
    )
    @ApiResponse(responseCode = "201", description = "예매 확정. 같은 멱등 키로 다시 보내면 결제 없이 그 예매를 돌려준다.")
    @ApiResponse(
        responseCode = "402",
        description = "PAYMENT_DECLINED(결제 거절. 좌석은 선점 그대로다)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "409",
        description = "PAYMENT_IN_PROGRESS(같은 선점에 결제 진행 중), BOOKING_TIME_EXPIRED(결제 중 예매 가능 시간이 끝났거나 공연이 시작되어 결제를 취소함. 회차가 시간 초과로 끝남), " +
            "HOLD_NOT_FOUND, HOLD_EXPIRED, PERFORMANCE_ALREADY_STARTED, IDEMPOTENCY_KEY_IN_PROGRESS, IDEMPOTENCY_KEY_ALREADY_FAILED",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "502",
        description = "PAYMENT_UNAVAILABLE(결제 결과를 받지 못함. 결제는 취소를 요청했고 좌석은 선점 그대로다)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun reserve(
        userId: Long,
        performanceId: Long,
        @Parameter(description = "결제 요청마다 화면이 만든 멱등 키(1~64자). 같은 요청을 다시 보낼 때는 같은 값을 쓴다.", required = true)
        @Size(min = 1, max = 64)
        idempotencyKey: String,
    ): ReservationResponse

    @Operation(summary = "확정한 예매", description = "사용자가 그 회차에서 확정한 예매. 회차 데이터가 지워진 뒤에는 404다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "404",
        description = "RESERVATION_NOT_FOUND",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getConfirmedReservation(userId: Long, performanceId: Long): ReservationResponse
}
