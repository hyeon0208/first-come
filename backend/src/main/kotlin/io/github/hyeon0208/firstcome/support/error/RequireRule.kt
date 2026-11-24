package io.github.hyeon0208.firstcome.support.error

// 도메인 규칙 검증. 규칙 위반은 잘못된 입력이므로 IllegalArgumentException(500)이 아니라 400 INVALID_REQUEST로 응답한다.
inline fun requireRule(condition: Boolean, message: () -> String) {
    if (!condition) {
        throw BusinessException(CommonErrorCode.INVALID_REQUEST, message())
    }
}

fun requireRule(condition: Boolean, errorCode: ErrorCode) {
    if (!condition) {
        throw BusinessException(errorCode)
    }
}
