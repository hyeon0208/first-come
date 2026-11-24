package io.github.hyeon0208.firstcome.user.application.dto

import io.github.hyeon0208.firstcome.user.domain.UserEntity

// 사용자를 만들 때 한 번만 나오는 세션 쿠키 값을 함께 담는다. 쿠키로만 내려 주고 응답 본문에는 넣지 않는다.
data class UserSessionResponse(
    val user: UserResponse,
    val sessionToken: String,
) {
    companion object {
        fun of(user: UserEntity, sessionToken: String): UserSessionResponse =
            UserSessionResponse(user = UserResponse.from(user), sessionToken = sessionToken)
    }
}
