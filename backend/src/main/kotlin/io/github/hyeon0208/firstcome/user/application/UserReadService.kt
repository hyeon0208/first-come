package io.github.hyeon0208.firstcome.user.application

import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.user.application.dto.UserResponse
import io.github.hyeon0208.firstcome.user.domain.SessionToken
import io.github.hyeon0208.firstcome.user.domain.UserErrorCode
import io.github.hyeon0208.firstcome.user.infrastructure.JpaUserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class UserReadService(
    private val userRepository: JpaUserRepository,
) {
    fun getUser(userId: Long): UserResponse =
        userRepository.findByIdOrNull(userId)?.let(UserResponse::from) ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

    fun findUserIdBySessionToken(sessionToken: String): Long? =
        userRepository.findBySessionTokenHash(SessionToken.hash(sessionToken))?.id
}
