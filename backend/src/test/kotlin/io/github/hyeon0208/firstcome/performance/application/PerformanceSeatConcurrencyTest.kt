package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.application.dto.PreselectResult
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.github.hyeon0208.firstcome.support.test.runConcurrently
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

// 실제 MySQL에서 여러 참가자가 같은 좌석을 동시에 노릴 때: 한 명만 가져가고, 교착이 없다.
class PerformanceSeatConcurrencyTest : BaseServiceTest() {
    @Autowired
    private lateinit var performanceSeatWriteService: PerformanceSeatWriteService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private fun joinParticipants(performanceId: Long, count: Int): List<Int> =
        (1..count).toList().onEach { performanceSeatWriteService.join(performanceId, it) }

    private fun ownersBySeatId(performanceId: Long): Map<Long, Int> =
        jdbcTemplate.query(
            "SELECT seat_id, participant_no FROM performance_seats WHERE performance_id = ? AND participant_no IS NOT NULL",
            { rs, _ -> rs.getLong("seat_id") to rs.getInt("participant_no") },
            performanceId,
        ).toMap()

    init {
        given("참가자 10명이 같은 좌석(1)을 동시에 누를 때") {
            val performanceId = fixture.enterPerformance(fixture.createUser())
            val participantNos = joinParticipants(performanceId, 10)

            `when`("동시에 가선점을 요청하면") {
                val results = runConcurrently(participantNos.map { no -> { performanceSeatWriteService.preselect(performanceId, no, 1) } })

                then("한 명만 가져가고 나머지 9명은 예외 없이 거절(TAKEN)되며, 좌석은 가져간 참가자의 것이다") {
                    results.mapNotNull { it.exceptionOrNull() }.shouldBeEmpty()
                    val outcomes = results.map { it.getOrThrow() }
                    outcomes.count { it == PreselectResult.SELECTED } shouldBe 1
                    outcomes.count { it == PreselectResult.TAKEN } shouldBe 9
                    val winnerNo = participantNos[outcomes.indexOf(PreselectResult.SELECTED)]
                    fixture.seatState(performanceId, 1).participantNo shouldBe winnerNo
                }
            }
        }

        given("참가자 20명이 서로 다른 좌석을 2개씩 고를 때") {
            val performanceId = fixture.enterPerformance(fixture.createUser())
            val participantNos = joinParticipants(performanceId, 20)

            `when`("동시에 가선점하고 선택 완료까지 하면") {
                val results = runConcurrently(
                    participantNos.map { no ->
                        {
                            val seatIds = listOf(no * 2L - 1, no * 2L)
                            seatIds.forEach { performanceSeatWriteService.preselect(performanceId, no, it) }
                            performanceSeatWriteService.hold(performanceId, no).heldSeatIds
                        }
                    },
                )

                then("교착이나 잠금 대기 초과 없이 모두 자기 좌석 2개를 선점한다") {
                    results.mapNotNull { it.exceptionOrNull()?.toString() }.shouldBeEmpty()
                    results.map { it.getOrThrow().size }.shouldContainOnly(2)
                    ownersBySeatId(performanceId).size shouldBe 40
                }
            }
        }

        given("참가자 30명이 좌석 20개(1~20)를 두고 겨룰 때") {
            val performanceId = fixture.enterPerformance(fixture.createUser())
            val participantNos = joinParticipants(performanceId, 30)

            `when`("각자 겹치는 좌석 3개를 동시에 누르고 잡은 좌석으로 선택 완료하기를 3회 반복하면") {
                val failures = (1..3).flatMap { round ->
                    runConcurrently(
                        participantNos.map { no ->
                            {
                                val seatIds = (0..2).map { (no + round + it * 7) % 20 + 1L }
                                val selected = seatIds.count { performanceSeatWriteService.preselect(performanceId, no, it) == PreselectResult.SELECTED }
                                if (selected > 0) performanceSeatWriteService.hold(performanceId, no)
                            }
                        },
                    ).mapNotNull { it.exceptionOrNull()?.toString() }
                }

                then("교착 없이 끝난다(거절은 예외가 아니라 결과다). 이미 선점한 참가자가 다시 누르는 경우만 ALREADY_HOLDING이다") {
                    failures.filterNot { "이미 선점한 좌석이 있습니다" in it }.shouldBeEmpty()
                }
                then("20개 좌석이 모두 팔리고 좌석마다 주인은 한 명이다") {
                    ownersBySeatId(performanceId).keys shouldBe (1L..20L).toSet()
                }
            }
        }
    }
}
