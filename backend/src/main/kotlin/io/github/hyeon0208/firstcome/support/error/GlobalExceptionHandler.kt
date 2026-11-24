package io.github.hyeon0208.firstcome.support.error

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.transaction.CannotCreateTransactionException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingRequestHeaderException
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
        MissingRequestHeaderException::class,
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

    @ExceptionHandler(PessimisticLockingFailureException::class)
    fun handleLockConflict(exception: PessimisticLockingFailureException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
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
        logger.error(
            "요청 처리 실패 method={} uri={} status={} code={} detail={}",
            request.method,
            request.requestURI,
            errorCode.status.value(),
            errorCode.name,
            detail,
            cause,
        )
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse(code = errorCode.name, message = message))
    }
}
