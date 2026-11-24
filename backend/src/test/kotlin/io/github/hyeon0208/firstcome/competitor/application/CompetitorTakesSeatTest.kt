package io.github.hyeon0208.firstcome.competitor.application

import io.github.hyeon0208.firstcome.performance.application.BookingFacade
import io.github.hyeon0208.firstcome.performance.application.PerformanceWriteService
import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseSpringTest
import io.github.hyeon0208.firstcome.support.test.MySqlContainerConfiguration
import io.github.hyeon0208.firstcome.support.test.TestBeanConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

// 가상 경쟁자는 화면 연출이 아니다. 서버 안에서 사용자와 같은 경로로 좌석을 가져가고,
// 그 좌석을 사용자가 누르면 서버가 거절한다.
@SpringBootTest(
    properties = [
        "competitor.enabled=true",
        "booking.open-delay=0s",
        "competitor.press-interval-min=0ms",
        "competitor.press-interval-max=0ms",
        "competitor.checkout-delay-min=0ms",
        "competitor.checkout-delay-max=0ms",
        "competitor.difficulties.easy.count=40",
        "competitor.difficulties.easy.arrival-window=300ms",
        "competitor.difficulties.normal.count=300",
        "competitor.difficulties.normal.arrival-window=6s",
    ],
)
@ActiveProfiles("test")
@Import(TestBeanConfiguration::class, MySqlContainerConfiguration::class)
class CompetitorTakesSeatTest : BaseSpringTest() {
    @Autowired
    private lateinit var bookingFacade: BookingFacade

    @Autowired
    private lateinit var performanceWriteService: PerformanceWriteService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private fun seatCount(performanceId: Long, status: String): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM performance_seats WHERE performance_id = ? AND status = ?", Int::class.java, performanceId, status)!!

    private fun takenSeatCount(performanceId: Long): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM performance_seats WHERE performance_id = ? AND status <> 'AVAILABLE'", Int::class.java, performanceId)!!

    // 가상 경쟁자가 모두 결제를 끝낼 때까지 기다린다(잡은 좌석이 모두 예매 완료가 되고 더 늘지 않을 때).
    private fun awaitCompetitorsDone(performanceId: Long) {
        var stableChecks = 0
        var last = -1
        repeat(200) {
            val reserved = seatCount(performanceId, "RESERVED")
            stableChecks = if (reserved > 0 && reserved == last && reserved == takenSeatCount(performanceId)) stableChecks + 1 else 0
            if (stableChecks >= 5) return
            last = reserved
            Thread.sleep(100)
        }
        error("가상 경쟁자가 20초 안에 예매를 마치지 않았습니다. reserved=$last taken=${takenSeatCount(performanceId)}")
    }

    init {
        given("사용자가 쉬움 난이도(가상 경쟁자 40명)로 연습을 시작하고, 가상 경쟁자들이 예매를 마쳤을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.createPerformance(userId)
            awaitCompetitorsDone(performanceId)
            bookingFacade.enter(userId, performanceId)
            val competitorSeatId = jdbcTemplate.queryForObject(
                "SELECT MIN(seat_id) FROM performance_seats WHERE performance_id = ? AND status = 'RESERVED'",
                Long::class.java,
                performanceId,
            )!!

            `when`("회차의 좌석을 보면") {
                val reserved = seatCount(performanceId, "RESERVED")

                then("가상 경쟁자들이 좌석을 가져가 예매 완료까지 갔다(40명이 1~4석씩)") {
                    reserved shouldBeGreaterThan 40
                    reserved shouldBeLessThan 161
                    takenSeatCount(performanceId) shouldBe reserved
                }
                then("그 좌석들은 사용자와 같은 결제 경로를 지났다(결제 승인과 확정된 예매가 남는다)") {
                    val confirmed = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM reservations WHERE performance_id = ? AND status = 'CONFIRMED' AND participant_no > 0",
                        Int::class.java,
                        performanceId,
                    )!!
                    confirmed shouldBeGreaterThan 0
                    fakePaymentClient.approvedRequests.size shouldBe confirmed
                    jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM reservation_seats WHERE performance_id = ? AND active = TRUE",
                        Int::class.java,
                        performanceId,
                    ) shouldBe reserved
                }
            }

            `when`("가상 경쟁자가 먼저 잡은 좌석을 사용자가 누르면") {
                val exception = shouldThrow<BusinessException> { bookingFacade.preselect(userId, performanceId, competitorSeatId) }

                then("서버가 SEAT_TAKEN으로 거절하고, 그 좌석은 가상 경쟁자(0번이 아닌 참가자)의 것 그대로다") {
                    exception.errorCode shouldBe PerformanceErrorCode.SEAT_TAKEN
                    fixture.seatState(performanceId, competitorSeatId).participantNo shouldNotBe 0
                    fixture.seatState(performanceId, competitorSeatId).status shouldBe "RESERVED"
                }
                then("좌석 상태에는 가상 경쟁자가 잡은 좌석이 선택 불가로 나온다") {
                    bookingFacade.getSeatStatuses(userId, performanceId).unavailableSeatIds.size shouldBe seatCount(performanceId, "RESERVED")
                }
            }

            `when`("아무도 잡지 않은 좌석을 사용자가 누르면") {
                val freeSeatId = jdbcTemplate.queryForObject(
                    "SELECT MIN(seat_id) FROM performance_seats WHERE performance_id = ? AND status = 'AVAILABLE'",
                    Long::class.java,
                    performanceId,
                )!!
                val state = bookingFacade.preselect(userId, performanceId, freeSeatId)

                then("사용자가 가져간다") {
                    state.mySeatIds shouldBe listOf(freeSeatId)
                }
            }
        }

        given("가상 경쟁자 300명이 6초에 걸쳐 들어오는 회차에서") {
            val userId = fixture.createUser()
            val performanceId = fixture.createPerformance(userId, difficulty = io.github.hyeon0208.firstcome.performance.Difficulty.NORMAL)
            Thread.sleep(1_000)

            `when`("사용자가 1초 만에 회차를 중단하면") {
                performanceWriteService.abandon(userId)
                Thread.sleep(500)
                val takenAfterStop = takenSeatCount(performanceId)
                Thread.sleep(1_500)

                then("남은 가상 경쟁자가 멈춰 좌석이 더 팔리지 않는다") {
                    takenAfterStop shouldBeGreaterThan 0
                    takenSeatCount(performanceId) shouldBe takenAfterStop
                    takenAfterStop shouldBeLessThan 500
                }
            }
        }
    }
}
