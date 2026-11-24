package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.support.error.ErrorCode
import org.springframework.http.HttpStatus

enum class VenueErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    VENUE_NOT_FOUND(HttpStatus.NOT_FOUND, "공연장을 찾을 수 없습니다."),
}
