package io.github.hyeon0208.firstcome.user.infrastructure

import io.github.hyeon0208.firstcome.user.domain.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface JpaUserRepository : JpaRepository<UserEntity, Long> {
    fun findBySessionTokenHash(sessionTokenHash: String): UserEntity?
}
