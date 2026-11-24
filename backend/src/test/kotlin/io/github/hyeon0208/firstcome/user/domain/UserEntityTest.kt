package io.github.hyeon0208.firstcome.user.domain

import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class UserEntityTest : BehaviorSpec({
    given("닉네임 규칙(앞뒤 공백을 빼고 2~12자)으로") {
        `when`("2자, 12자 닉네임으로 사용자를 만들면") {
            then("만들어지고 앞뒤 공백은 빠진다") {
                UserEntity(nickname = "  연습  ", sessionTokenHash = "hash").nickname shouldBe "연습"
                UserEntity(nickname = "가".repeat(12), sessionTokenHash = "hash").nickname.length shouldBe 12
            }
        }

        `when`("1자, 13자, 공백만으로 사용자를 만들면") {
            then("거절된다") {
                listOf("가", "가".repeat(13), "    ").forEach {
                    shouldThrow<BusinessException> { UserEntity(nickname = it, sessionTokenHash = "hash") }
                        .message shouldBe "닉네임은 2~12자로 입력해 주세요."
                }
            }
        }
    }

    given("세션 쿠키 값으로") {
        `when`("두 번 만들면") {
            val first = SessionToken.generate()
            val second = SessionToken.generate()

            then("추측할 수 없게 매번 다른 64자 값이 나오고, 해시는 원래 값과 다르다") {
                first shouldNotBe second
                first.length shouldBe 64
                SessionToken.hash(first) shouldNotBe first
                SessionToken.hash(first) shouldBe SessionToken.hash(first)
            }
        }
    }
})
