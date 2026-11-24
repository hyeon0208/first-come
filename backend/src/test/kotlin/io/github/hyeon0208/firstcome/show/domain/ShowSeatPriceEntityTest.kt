package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.error.CommonErrorCode
import io.github.hyeon0208.firstcome.show.domain.SeatGrade
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ShowSeatPriceEntityTest : BehaviorSpec({
    given("등급별 가격 규칙(0원 이상)으로") {
        `when`("0원으로 등급별 가격을 만들면") {
            val price = ShowSeatPriceEntity(showId = 1, grade = SeatGrade.VIP, price = 0)

            then("만들어진다") {
                price.price shouldBe 0
            }
        }

        `when`("음수 가격으로 등급별 가격을 만들면") {
            val exception = shouldThrow<BusinessException> {
                ShowSeatPriceEntity(showId = 1, grade = SeatGrade.VIP, price = -1)
            }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }
    }
})
