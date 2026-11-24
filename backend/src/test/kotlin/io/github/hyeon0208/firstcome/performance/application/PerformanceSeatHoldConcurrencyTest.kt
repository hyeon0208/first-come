package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.github.hyeon0208.firstcome.support.test.runConcurrently
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager

class PerformanceSeatHoldConcurrencyTest : BaseServiceTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private fun heldSeatIds(userId: Long): List<Long> =
        jdbcTemplate.query(
            "SELECT id FROM performance_seats WHERE holder_id = ? AND status = 'HELD' ORDER BY id",
            { rs, _ -> rs.getLong("id") },
            userId,
        )

    // 성공이면 "성공", 업무 오류면 오류 코드, 그 밖의 예외(교착 등)면 예외 클래스 이름
    private fun outcome(result: Result<*>): String =
        result.exceptionOrNull()?.let { (it as? BusinessException)?.errorCode?.name ?: it.javaClass.simpleName } ?: "성공"

    init {
        given("10명이 같은 좌석(1)을 동시에 노릴 때") {
            loadInitialData()
            val userIds = List(10) { fixture.createUser() }

            `when`("동시에 임시 배정을 요청하면") {
                val results = runConcurrently(userIds.map { userId -> { fixture.holdSeats(userId, performanceId = 1, listOf(1)) } })

                then("한 명만 성공하고 나머지 9명은 SEAT_NOT_AVAILABLE이며, 좌석은 성공한 사람의 것이다") {
                    results.count { it.isSuccess } shouldBe 1
                    results.mapNotNull { (it.exceptionOrNull() as? BusinessException)?.errorCode } shouldBe
                        List(9) { PerformanceErrorCode.SEAT_NOT_AVAILABLE }
                    val winnerId = userIds[results.indexOfFirst { it.isSuccess }]
                    heldSeatIds(winnerId) shouldBe listOf(1L)
                }
            }
        }

        given("10명이 서로 다른 좌석을 고를 때") {
            loadInitialData()

            `when`("매 회 새 사용자 10명이 동시에 임시 배정을 요청하기를 5회 반복하면") {
                val failures = (1..5).flatMap { round ->
                    val userIds = List(10) { fixture.createUser() }
                    val tasks = userIds.mapIndexed { index, userId -> { fixture.holdSeats(userId, performanceId = 1, listOf(((round - 1) * 20 + index + 1).toLong())) } }
                    runConcurrently(tasks).mapNotNull { it.exceptionOrNull() }
                }

                then("Gap 교착 없이 모두 성공한다") {
                    failures.map { it.message } shouldBe emptyList()
                }
            }
        }

        given("같은 사용자가 두 묶음을 고를 때") {
            loadInitialData()
            val userId = fixture.createUser()

            `when`("{1,2,3}과 {4,5,6}을 동시에 요청하면") {
                val results = runConcurrently(listOf({ fixture.holdSeats(userId, performanceId = 1, listOf(1, 2, 3)) }, { fixture.holdSeats(userId, performanceId = 1, listOf(4, 5, 6)) }))

                then("요청이 하나씩 처리되어, 나중 묶음 3석만 임시 배정으로 남는다") {
                    results.count { it.isSuccess } shouldBe 2
                    heldSeatIds(userId) shouldBeIn listOf(listOf(1L, 2L, 3L), listOf(4L, 5L, 6L))
                }
            }
        }

        given("사용자 A가 좌석 15를 쥐고 좌석 11로 바꾸는 동안, 사용자 B가 좌석 11, 15를 함께 노릴 때") {
            loadInitialData()
            val changerId = fixture.createUser()
            val otherId = fixture.createUser()

            `when`("A의 변경(15 → 11)과 B의 요청(11, 15)을 동시에 보내기를 50회 반복하면") {
                val rounds = (1..50).map {
                    fixture.holdSeats(changerId, performanceId = 1, listOf(15))
                    runConcurrently(
                        listOf(
                            { fixture.holdSeats(changerId, performanceId = 1, listOf(11)) },
                            { fixture.holdSeats(otherId, performanceId = 1, listOf(11, 15)) },
                        ),
                    )
                }

                then("교착 없이 A는 매번 좌석을 바꾸고, B는 SEAT_NOT_AVAILABLE로만 실패한다") {
                    rounds.map { outcome(it[0]) } shouldBe List(50) { "성공" }
                    rounds.map { outcome(it[1]) } shouldBe List(50) { PerformanceErrorCode.SEAT_NOT_AVAILABLE.name }
                }
            }
        }
    }
}
