package io.github.hyeon0208.firstcome.user.application

import io.github.hyeon0208.firstcome.user.application.dto.CreateUserRequest
import io.github.hyeon0208.firstcome.user.application.dto.UserSessionResponse
import io.github.hyeon0208.firstcome.user.domain.SessionToken
import io.github.hyeon0208.firstcome.user.domain.UserEntity
import io.github.hyeon0208.firstcome.user.infrastructure.JpaUserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserWriteService(
    private val userRepository: JpaUserRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun createUser(request: CreateUserRequest): UserSessionResponse {
        val sessionToken = SessionToken.generate()
        val user = userRepository.save(UserEntity(nickname = request.nickname, sessionTokenHash = SessionToken.hash(sessionToken)))
        logger.info("사용자 등록 userId={}", user.id)
        return UserSessionResponse.of(user, sessionToken)
    }
}
