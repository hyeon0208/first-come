package io.github.hyeon0208.firstcome.user.application.dto

import io.github.hyeon0208.firstcome.user.domain.UserEntity

data class UserResponse(
    val id: Long,
    val nickname: String,
) {
    companion object {
        fun from(user: UserEntity): UserResponse =
            UserResponse(id = user.id, nickname = user.nickname)
    }
}
