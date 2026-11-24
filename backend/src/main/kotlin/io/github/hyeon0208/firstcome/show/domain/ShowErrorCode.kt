package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.support.error.ErrorCode
import org.springframework.http.HttpStatus

enum class ShowErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    SHOW_NOT_FOUND(HttpStatus.NOT_FOUND, "공연을 찾을 수 없습니다."),
}
