package io.github.hyeon0208.firstcome.support.error

class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
) : RuntimeException(message)
