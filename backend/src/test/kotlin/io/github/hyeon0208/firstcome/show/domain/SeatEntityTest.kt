package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.error.CommonErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SeatEntityTest : BehaviorSpec({
    given("좌석 위치 규칙(행, 열은 1 이상)으로") {
        `when`("1행 1열 좌석을 만들면") {
            val seat = SeatEntity(venueId = 1, row = 1, column = 1, grade = SeatGrade.VIP)

            then("만들어진다") {
                seat.row shouldBe 1
                seat.column shouldBe 1
            }
        }

        `when`("0행 좌석을 만들면") {
            val exception = shouldThrow<BusinessException> {
                SeatEntity(venueId = 1, row = 0, column = 1, grade = SeatGrade.VIP)
            }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }

        `when`("0열 좌석을 만들면") {
            val exception = shouldThrow<BusinessException> {
                SeatEntity(venueId = 1, row = 1, column = 0, grade = SeatGrade.VIP)
            }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }
    }

})
