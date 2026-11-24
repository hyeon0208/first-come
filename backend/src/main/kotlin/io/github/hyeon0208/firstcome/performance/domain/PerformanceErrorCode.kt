package io.github.hyeon0208.firstcome.performance.domain

import io.github.hyeon0208.firstcome.support.error.ErrorCode
import org.springframework.http.HttpStatus

enum class PerformanceErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "공연 회차를 찾을 수 없습니다."),
    PERFORMANCE_ALREADY_STARTED(HttpStatus.CONFLICT, "이미 시작한 공연 회차입니다."),
    SEAT_COUNT_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "한 번에 1~5석까지 임시 배정할 수 있습니다."),
    DUPLICATED_SEAT(HttpStatus.BAD_REQUEST, "같은 좌석을 두 번 요청했습니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "이 회차에 없는 좌석입니다."),
    SEAT_NOT_AVAILABLE(HttpStatus.CONFLICT, "이미 임시 배정되었거나 예약된 좌석이 있습니다."),
}
