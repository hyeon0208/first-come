package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatStatus
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.github.hyeon0208.firstcome.show.domain.SeatGrade
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class PerformanceSeatReadServiceTest : BaseServiceTest() {
    @Autowired
    private lateinit var performanceSeatReadService: PerformanceSeatReadService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        given("초기 데이터가 있을 때") {
            loadInitialData()

            `when`("모래시계 밴드 겨울 단독 공연 1회차 좌석을 조회하면") {
                val response = performanceSeatReadService.getPerformanceSeats(venueId = 1, showId = 1, performanceId = 1)

                then("400석을 행/열 순서로, 행 범위의 등급 가격으로 돌려준다") {
                    response.showTitle shouldBe "모래시계 밴드 겨울 단독 공연"
                    response.startAt shouldBe LocalDateTime.of(2026, 12, 25, 10, 0)
                    response.seats.map { it.row to it.column } shouldBe (1..20).flatMap { row -> (1..20).map { row to it } }
                    response.seats.filter { it.column == 1 }.map { it.grade to it.price } shouldBe
                        List(3) { SeatGrade.VIP to 150_000L } + List(5) { SeatGrade.R to 120_000L } +
                        List(6) { SeatGrade.S to 90_000L } + List(6) { SeatGrade.A to 60_000L }
                    response.seats.forAll { it.status shouldBe PerformanceSeatStatus.AVAILABLE }
                }
            }

            `when`("유리온실 3집 발매 기념 콘서트 회차(4)를 모래시계 밴드 겨울 단독 공연(1) 경로로 조회하면") {
                val exception = shouldThrow<BusinessException> {
                    performanceSeatReadService.getPerformanceSeats(venueId = 1, showId = 1, performanceId = 4)
                }

                then("PERFORMANCE_NOT_FOUND로 거부된다") {
                    exception.errorCode shouldBe PerformanceErrorCode.PERFORMANCE_NOT_FOUND
                }
            }
        }

        given("임시 배정(만료 전/후)과 예약 완료 좌석이 있을 때") {
            loadInitialData()
            val now = LocalDateTime.now(testClock)
            jdbcTemplate.update("INSERT INTO users (id, name) VALUES (1, '홍길동')")
            jdbcTemplate.update(
                "UPDATE performance_seats SET status = 'HELD', holder_id = 1, hold_expires_at = ? WHERE id = 1",
                now.plusMinutes(1),
            )
            jdbcTemplate.update(
                "UPDATE performance_seats SET status = 'HELD', holder_id = 1, hold_expires_at = ? WHERE id = 2",
                now.minusSeconds(1),
            )
            jdbcTemplate.update("UPDATE performance_seats SET status = 'RESERVED' WHERE id = 3")

            `when`("좌석을 조회하면") {
                val response = performanceSeatReadService.getPerformanceSeats(venueId = 1, showId = 1, performanceId = 1)

                then("만료 전 임시 배정은 HELD, 만료된 임시 배정은 AVAILABLE, 예약 완료는 RESERVED다") {
                    response.seats.take(3).map { it.status } shouldBe listOf(
                        PerformanceSeatStatus.HELD,
                        PerformanceSeatStatus.AVAILABLE,
                        PerformanceSeatStatus.RESERVED,
                    )
                }
            }
        }
    }
}
