package io.github.hyeon0208.firstcome.user.api

import java.time.Duration
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

// 세션 쿠키. 스크립트가 읽지 못하게 HttpOnly로 내려 준다.
@Component
class SessionCookie(
    @Value("\${session.cookie-secure}") private val secure: Boolean,
) {
    fun of(sessionToken: String): ResponseCookie =
        ResponseCookie.from(NAME, sessionToken)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(MAX_AGE)
            .build()

    companion object {
        const val NAME = "fc_session"
        private val MAX_AGE: Duration = Duration.ofDays(365)
    }
}
