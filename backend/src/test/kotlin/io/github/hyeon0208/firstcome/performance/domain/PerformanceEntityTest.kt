package io.github.hyeon0208.firstcome.performance.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PerformanceEntityTest : BehaviorSpec({

    given("10시에 시작하는 회차가 있을 때") {
        val performance = PerformanceEntity(showId = 1, startAt = LocalDateTime.of(2026, 12, 25, 10, 0))

        `when`("시작 1초 전과 시작 시각에 시작 여부를 보면") {
            val beforeStart = performance.isStartedAt(LocalDateTime.of(2026, 12, 25, 9, 59, 59))
            val atStart = performance.isStartedAt(LocalDateTime.of(2026, 12, 25, 10, 0))

            then("시작 시각부터 시작된 것으로 본다") {
                beforeStart shouldBe false
                atStart shouldBe true
            }
        }
    }
})
