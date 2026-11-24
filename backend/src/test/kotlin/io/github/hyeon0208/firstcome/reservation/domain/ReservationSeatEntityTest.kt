package io.github.hyeon0208.firstcome.reservation.domain

import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.error.CommonErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ReservationSeatEntityTest : BehaviorSpec({
    given("예약 좌석 규칙(가격 0원 이상)으로") {
        `when`("0원으로 예약 좌석을 만들면") {
            val reservationSeat = ReservationSeatEntity(reservationId = 1, performanceId = 1, performanceSeatId = 1, price = 0)

            then("만들어진다") {
                reservationSeat.price shouldBe 0L
            }
        }

        `when`("음수 가격으로 예약 좌석을 만들면") {
            val exception = shouldThrow<BusinessException> { ReservationSeatEntity(reservationId = 1, performanceId = 1, performanceSeatId = 1, price = -1) }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }
    }
})
