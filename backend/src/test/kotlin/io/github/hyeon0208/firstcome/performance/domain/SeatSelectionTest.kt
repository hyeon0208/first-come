package io.github.hyeon0208.firstcome.performance.domain

import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SeatSelectionTest : BehaviorSpec({
    given("좌석 선택 규칙(1~5석, 중복 없음)으로") {
        `when`("좌석 5개로 선택을 만들면") {
            val selection = SeatSelection(listOf(1, 2, 3, 4, 5))

            then("만들어진다") {
                selection.performanceSeatIds.size shouldBe 5
            }
        }

        `when`("좌석 0개로 선택을 만들면") {
            val exception = shouldThrow<BusinessException> { SeatSelection(emptyList()) }

            then("SEAT_COUNT_OUT_OF_RANGE로 거부된다") {
                exception.errorCode shouldBe PerformanceErrorCode.SEAT_COUNT_OUT_OF_RANGE
            }
        }

        `when`("좌석 6개로 선택을 만들면") {
            val exception = shouldThrow<BusinessException> { SeatSelection(listOf(1, 2, 3, 4, 5, 6)) }

            then("SEAT_COUNT_OUT_OF_RANGE로 거부된다") {
                exception.errorCode shouldBe PerformanceErrorCode.SEAT_COUNT_OUT_OF_RANGE
            }
        }

        `when`("같은 좌석을 두 번 넣어 선택을 만들면") {
            val exception = shouldThrow<BusinessException> { SeatSelection(listOf(1, 1)) }

            then("DUPLICATED_SEAT로 거부된다") {
                exception.errorCode shouldBe PerformanceErrorCode.DUPLICATED_SEAT
            }
        }
    }

})
