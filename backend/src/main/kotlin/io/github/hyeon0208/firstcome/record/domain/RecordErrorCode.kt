package io.github.hyeon0208.firstcome.record.domain

import io.github.hyeon0208.firstcome.support.error.ErrorCode
import org.springframework.http.HttpStatus

enum class RecordErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "연습 기록을 찾을 수 없습니다."),
}
