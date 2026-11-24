package io.github.hyeon0208.firstcome.support.error

import org.springframework.http.HttpStatus

interface ErrorCode {
    val name: String
    val status: HttpStatus
    val message: String
}
