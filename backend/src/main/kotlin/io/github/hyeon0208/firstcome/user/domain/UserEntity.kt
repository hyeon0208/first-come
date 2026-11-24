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
    nickname: String,

    @Column(nullable = false, length = 64)
    val sessionTokenHash: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @Column(nullable = false, length = NICKNAME_MAX_LENGTH)
    val nickname: String = nickname.trim()

    init {
        requireRule(this.nickname.length in NICKNAME_MIN_LENGTH..NICKNAME_MAX_LENGTH) {
            "닉네임은 $NICKNAME_MIN_LENGTH~${NICKNAME_MAX_LENGTH}자로 입력해 주세요."
        }
    }

    companion object {
        const val NICKNAME_MIN_LENGTH = 2
        const val NICKNAME_MAX_LENGTH = 12
    }
}
