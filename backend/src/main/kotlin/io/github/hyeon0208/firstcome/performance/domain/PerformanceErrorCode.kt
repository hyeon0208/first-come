package io.github.hyeon0208.firstcome.performance.domain

import io.github.hyeon0208.firstcome.support.error.ErrorCode
import org.springframework.http.HttpStatus

enum class PerformanceErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "이미 진행 중인 예매가 있습니다."),
    PRACTICE_FULL(HttpStatus.SERVICE_UNAVAILABLE, "지금은 연습 자리가 모두 찼어요. 잠시 후 다시 시도해 주세요."),
    NOT_OPEN_YET(HttpStatus.CONFLICT, "판매 시작 시간 이전입니다."),
    NOT_ENTERED(HttpStatus.CONFLICT, "예매 화면에 먼저 입장해야 합니다."),
    SESSION_EXPIRED(HttpStatus.GONE, "세션이 만료되었습니다."),
    TIME_OVER(HttpStatus.GONE, "제한 시간이 지나 처음부터 다시 예매해야 합니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "이 회차에 없는 좌석입니다."),
    SEAT_TAKEN(HttpStatus.CONFLICT, "선택하신 좌석이 이미 선점되었습니다. 다른 좌석을 선택해주세요."),
    SOLD_OUT(HttpStatus.CONFLICT, "아쉽게도 선택하신 회차에 준비된 티켓이 모두 판매 완료되었어요."),
    SEAT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "선택 가능한 매수를 초과했어요."),
    NO_SEATS_SELECTED(HttpStatus.BAD_REQUEST, "선택한 좌석이 없습니다."),
    ALREADY_HOLDING(HttpStatus.CONFLICT, "이미 선점한 좌석이 있습니다. 좌석을 다시 고르려면 좌석 선택 단계로 돌아가야 합니다."),
    HOLD_NOT_FOUND(HttpStatus.CONFLICT, "선점한 좌석이 없습니다."),
    BACK_BLOCKED(HttpStatus.CONFLICT, "예매 가능 시간이 얼마 남지 않아서 좌석선택 단계로 이동할 수 없어요."),
    SEATS_PAYING(HttpStatus.CONFLICT, "결제를 진행 중인 좌석이 있어 좌석을 바꿀 수 없습니다."),
    PRICE_COUNT_MISMATCH(HttpStatus.BAD_REQUEST, "가격 종류별 수량이 선점한 좌석 수와 같아야 합니다."),
}
