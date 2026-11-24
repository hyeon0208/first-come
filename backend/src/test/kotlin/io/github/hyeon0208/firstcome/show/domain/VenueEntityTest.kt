package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.error.CommonErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class VenueEntityTest : BehaviorSpec({
    given("공연장 이름 규칙(공백이 아닌 1~100자)으로") {
        `when`("100자 이름으로 공연장을 만들면") {
            val venue = VenueEntity(name = "가".repeat(100))

            then("만들어진다") {
                venue.name.length shouldBe 100
            }
        }

        `when`("공백만 있는 이름으로 공연장을 만들면") {
            val exception = shouldThrow<BusinessException> { VenueEntity(name = " ") }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }

        `when`("101자 이름으로 공연장을 만들면") {
            val exception = shouldThrow<BusinessException> { VenueEntity(name = "가".repeat(101)) }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }
    }
})
