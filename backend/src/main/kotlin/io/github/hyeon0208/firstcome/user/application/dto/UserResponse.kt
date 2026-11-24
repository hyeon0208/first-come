package io.github.hyeon0208.firstcome.user.application.dto

import io.github.hyeon0208.firstcome.user.domain.UserEntity

data class UserResponse(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(user: UserEntity): UserResponse =
            UserResponse(
                id = user.id,
                name = user.name,
            )
    }
}
