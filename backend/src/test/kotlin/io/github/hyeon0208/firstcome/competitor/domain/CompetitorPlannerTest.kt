package io.github.hyeon0208.firstcome.competitor.domain

import io.github.hyeon0208.firstcome.show.SeatGrade
import io.github.hyeon0208.firstcome.show.application.dto.SeatResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeSortedBy
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Duration
import kotlin.random.Random

class CompetitorPlannerTest : BehaviorSpec({
    fun seat(seatId: Long, floor: Int, block: String, rowNo: Int, seatNo: Int) =
        SeatResponse(seatId, floor, block, rowNo, seatNo, "${floor}층 ${block}구역 ${rowNo}열 ${seatNo}번", SeatGrade.R, "R석", 100_000)

    given("가상 경쟁자 500명이 2분에 걸쳐 들어오는 난이도로") {
        val plans = CompetitorPlanner.plan(seed = 42, count = 500, arrivalWindow = Duration.ofMinutes(2), maxSeats = 4)

        `when`("계획을 만들면") {
            then("500명이 1번부터 번호를 받고, 들어오는 시각은 2분 안이며 시각 순서로 나온다") {
                plans.map { it.participantNo }.sorted() shouldBe (1..500).toList()
                plans.all { it.arrivalOffset <= Duration.ofMinutes(2) } shouldBe true
                plans.shouldBeSortedBy { it.arrivalOffset }
            }
            then("한 명이 1~4석을 노리고 평균은 2.5석 안팎이다") {
                plans.all { it.wantedSeats in 1..4 } shouldBe true
                plans.sumOf { it.wantedSeats } shouldBeInRange 1_150..1_350
            }
        }

        `when`("같은 씨앗으로 다시 만들면") {
            then("같은 계획이 나오고, 다른 씨앗이면 다른 계획이 나온다") {
                CompetitorPlanner.plan(42, 500, Duration.ofMinutes(2), 4) shouldBe plans
                CompetitorPlanner.plan(43, 500, Duration.ofMinutes(2), 4) shouldNotBe plans
            }
        }
    }

    given("쉬움 난이도(200명)로") {
        `when`("씨앗 20개로 계획을 만들면") {
            val demands = (1L..20L).map { seed -> CompetitorPlanner.plan(seed, 200, Duration.ofMinutes(5), 4).sumOf { it.wantedSeats } }

            then("노리는 좌석의 합이 1,008석의 40~60%라 매진되지 않는다") {
                demands.forEach { it shouldBeInRange 403..605 }
            }
        }
    }

    given("1층 A구역 1열과 2층 C구역 4열에 좌석이 같은 수만큼 있을 때") {
        val front = (1..10).map { seat(it.toLong(), 1, "A", 1, it) }
        val back = (11..20).map { seat(it.toLong(), 2, "C", 4, it - 10) }
        val picker = SeatPicker(front + back)

        `when`("첫 좌석을 1,000번 고르면") {
            val random = Random(7)
            val frontPicks = (1..1_000).count { picker.pick(random, emptySet(), emptyList())!!.floor == 1 }

            then("무대에 가까운 좌석을 훨씬 자주 고른다(약 6배)") {
                frontPicks shouldBeInRange 800..920
            }
        }

        `when`("1열 5번을 잡은 뒤 다음 좌석을 고르면") {
            val next = picker.pick(Random(7), takenSeatIds = setOf(5L, 6L), selected = listOf(front[4]))!!

            then("같은 열에서 가장 가까운 빈자리(4번)를 고른다") {
                next.seatId shouldBe 4L
            }
        }

        `when`("모든 좌석이 팔린 것으로 알고 있으면") {
            then("고를 좌석이 없다") {
                picker.pick(Random(7), (1L..20L).toSet(), emptyList()) shouldBe null
            }
        }
    }
})
