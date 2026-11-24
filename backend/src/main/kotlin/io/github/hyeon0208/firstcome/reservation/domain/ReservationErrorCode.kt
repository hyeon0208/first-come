package io.github.hyeon0208.firstcome.reservation.domain

import io.github.hyeon0208.firstcome.support.error.ErrorCode
import org.springframework.http.HttpStatus

enum class ReservationErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    PAYMENT_DECLINED(HttpStatus.PAYMENT_REQUIRED, "결제가 거절되었습니다."),
    PAYMENT_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "결제 진행 중 오류가 발생했습니다."),
    PAYMENT_IN_PROGRESS(HttpStatus.CONFLICT, "이전 요청을 처리 중이에요. 잠시 후 다시 시도해 주세요."),
    BOOKING_TIME_EXPIRED(HttpStatus.CONFLICT, "결제할 수 있는 시간이 종료되어 결제를 자동으로 취소했습니다."),
    SESSION_EXPIRED(HttpStatus.GONE, "세션이 만료되었습니다."),
    HOLD_NOT_FOUND(HttpStatus.CONFLICT, "선점한 좌석이 없습니다."),
    HOLD_EXPIRED(HttpStatus.CONFLICT, "결제할 수 있는 시간이 종료되었어요."),
    PERFORMANCE_ALREADY_STARTED(HttpStatus.CONFLICT, "이미 시작한 공연입니다."),
    IDEMPOTENCY_KEY_IN_PROGRESS(HttpStatus.CONFLICT, "같은 멱등 키의 예매 요청을 처리하고 있습니다."),
    IDEMPOTENCY_KEY_ALREADY_FAILED(HttpStatus.CONFLICT, "실패로 끝난 멱등 키입니다. 새 멱등 키로 다시 요청해 주세요."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예매 내역을 찾을 수 없습니다."),
}
