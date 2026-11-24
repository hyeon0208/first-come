package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.show.domain.SeatGrade
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ShowSeatPricesTest : BehaviorSpec({
    given("VIP, R 등급 가격만 있는 공연 1의 가격표로") {
        val prices = ShowSeatPrices(
            showId = 1,
            prices = listOf(
                ShowSeatPriceEntity(showId = 1, grade = SeatGrade.VIP, price = 150_000),
                ShowSeatPriceEntity(showId = 1, grade = SeatGrade.R, price = 120_000),
            ),
        )

        `when`("VIP 가격을 찾으면") {
            val price = prices.priceOf(SeatGrade.VIP)

            then("그 등급의 가격을 돌려준다") {
                price shouldBe 150_000L
            }
        }

        `when`("가격이 없는 S 등급을 찾으면") {
            val exception = shouldThrow<IllegalStateException> { prices.priceOf(SeatGrade.S) }

            then("공연과 등급을 담은 오류로 실패한다") {
                exception.message shouldBe "공연에 좌석 등급 가격이 없습니다. showId=1 grade=S"
            }
        }
    }
})
