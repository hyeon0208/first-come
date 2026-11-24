package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.error.CommonErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ShowEntityTest : BehaviorSpec({
    given("공연 제목 규칙(공백이 아닌 1~100자)으로") {
        `when`("100자 제목으로 공연을 만들면") {
            val show = ShowEntity(venueId = 1, title = "가".repeat(100))

            then("만들어진다") {
                show.title.length shouldBe 100
            }
        }

        `when`("공백만 있는 제목으로 공연을 만들면") {
            val exception = shouldThrow<BusinessException> { ShowEntity(venueId = 1, title = " ") }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }

        `when`("101자 제목으로 공연을 만들면") {
            val exception = shouldThrow<BusinessException> { ShowEntity(venueId = 1, title = "가".repeat(101)) }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }
    }
})
