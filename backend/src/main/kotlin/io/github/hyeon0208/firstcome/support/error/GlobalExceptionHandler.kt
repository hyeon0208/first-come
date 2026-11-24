package io.github.hyeon0208.firstcome.support.error

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.core.NestedRuntimeException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.transaction.CannotCreateTransactionException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(exception: BusinessException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        response(request, exception.errorCode, detail = exception.message, message = exception.message)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(exception: HttpMessageNotReadableException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        response(request, CommonErrorCode.INVALID_REQUEST, detail = "요청 본문을 읽을 수 없음(${exception.mostSpecificCause.javaClass.simpleName})")

    @ExceptionHandler(
        HandlerMethodValidationException::class,
        MethodArgumentNotValidException::class,
        MissingRequestHeaderException::class,
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleInvalidRequest(exception: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        response(request, CommonErrorCode.INVALID_REQUEST, detail = exception.message)

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(exception: NoResourceFoundException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        response(request, CommonErrorCode.RESOURCE_NOT_FOUND, detail = exception.message)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(exception: HttpRequestMethodNotSupportedException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        response(request, CommonErrorCode.METHOD_NOT_ALLOWED, detail = exception.message)

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaType(exception: HttpMediaTypeNotSupportedException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        response(request, CommonErrorCode.UNSUPPORTED_MEDIA_TYPE, detail = exception.message)

    // 잠금 대기 초과·교착, 그리고 유일 제약이 막은 동시 요청은 다시 시도하면 되는 충돌이다.
    @ExceptionHandler(PessimisticLockingFailureException::class, DataIntegrityViolationException::class)
    fun handleLockConflict(exception: NestedRuntimeException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        response(request, CommonErrorCode.LOCK_CONFLICT, detail = exception.mostSpecificCause.message)

    @ExceptionHandler(CannotCreateTransactionException::class)
    fun handleUnavailable(exception: CannotCreateTransactionException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        response(request, CommonErrorCode.SERVICE_UNAVAILABLE, detail = exception.mostSpecificCause.message)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        response(request, CommonErrorCode.INTERNAL_ERROR, detail = exception.message, cause = exception)

    private fun response(
        request: HttpServletRequest,
        errorCode: ErrorCode,
        detail: String?,
        message: String = errorCode.message,
        cause: Throwable? = null,
    ): ResponseEntity<ErrorResponse> {
        val logFormat = "요청 거절 method={} uri={} status={} code={} detail={}"
        val arguments = arrayOf(request.method, request.requestURI, errorCode.status.value(), errorCode.name, detail, cause)
        // 4xx는 예매 중에 흔히 만나는 거절(이미 잡힌 좌석 등)이라 INFO로, 서버 쪽 문제만 ERROR로 남긴다.
        if (errorCode.status.is5xxServerError) logger.error(logFormat, *arguments) else logger.info(logFormat, *arguments)
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse(code = errorCode.name, message = message))
    }
}
