package io.github.hyeon0208.firstcome.user.domain

import io.github.hyeon0208.firstcome.support.domain.BaseEntity
import io.github.hyeon0208.firstcome.support.error.requireRule
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class UserEntity(
    name: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    val name: String = name.trim()

    init {
        requireRule(this.name.length in 1..NAME_MAX_LENGTH) { "이름은 앞뒤 공백을 빼고 1~${NAME_MAX_LENGTH}자여야 합니다." }
    }

    companion object {
        const val NAME_MAX_LENGTH = 50
    }
}
