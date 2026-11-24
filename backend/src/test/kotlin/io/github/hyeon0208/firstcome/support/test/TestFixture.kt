package io.github.hyeon0208.firstcome.support.test

import io.github.hyeon0208.firstcome.performance.Difficulty
import io.github.hyeon0208.firstcome.performance.application.BookingFacade
import io.github.hyeon0208.firstcome.performance.application.BookingProperties
import io.github.hyeon0208.firstcome.performance.application.PerformanceSeatWriteService
import io.github.hyeon0208.firstcome.performance.application.PerformanceWriteService
import io.github.hyeon0208.firstcome.performance.application.dto.CreatePerformanceRequest
import io.github.hyeon0208.firstcome.performance.application.dto.HoldSeatsResponse
import io.github.hyeon0208.firstcome.reservation.application.ReservationFacade
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.user.application.UserWriteService
import io.github.hyeon0208.firstcome.user.application.dto.CreateUserRequest
import java.time.LocalDateTime
import org.springframework.jdbc.core.JdbcTemplate

// 좌석 번호는 고정 데이터 순서대로다: 1층 A구역 1~192(1열이 1~16), B구역 193~384, C구역 385~576, D구역 577~768, 2층 769~1008.
// 공연 1의 가격은 R석(1층 A·B) 132,000원, S석 99,000원이다.
class TestFixture(
    private val userWriteService: UserWriteService,
    private val performanceWriteService: PerformanceWriteService,
    private val performanceSeatWriteService: PerformanceSeatWriteService,
    private val bookingFacade: BookingFacade,
    private val reservationFacade: ReservationFacade,
    private val bookingProperties: BookingProperties,
    private val jdbcTemplate: JdbcTemplate,
    private val testClock: TestClock,
) {
    fun createUser(): Long =
        userWriteService.createUser(CreateUserRequest(nickname = "연습생")).user.id

    fun createPerformance(userId: Long, showId: Long = 1, difficulty: Difficulty = Difficulty.EASY): Long =
        performanceWriteService.createPerformance(userId, CreatePerformanceRequest(showId = showId, difficulty = difficulty)).id

    // 연습을 시작하고, 예매 오픈 시각까지 시계를 옮긴 뒤 예매 화면에 들어온다.
    fun enterPerformance(userId: Long, showId: Long = 1): Long {
        val performanceId = createPerformance(userId, showId)
        testClock.advance(bookingProperties.openDelay)
        bookingFacade.enter(userId, performanceId)
        return performanceId
    }

    // 사용자가 좌석을 누르고(가선점) 선택 완료(선점)까지 한다.
    fun holdSeats(userId: Long, performanceId: Long, seatIds: List<Long>) {
        seatIds.forEach { bookingFacade.preselect(userId, performanceId, it) }
        bookingFacade.hold(userId, performanceId)
    }

    // 사용자가 아닌 다른 참가자가 좌석을 가선점하고 선점한다.
    fun holdSeatsAs(performanceId: Long, participantNo: Int, seatIds: List<Long>): HoldSeatsResponse {
        performanceSeatWriteService.join(performanceId, participantNo)
        seatIds.forEach { performanceSeatWriteService.preselect(performanceId, participantNo, it) }
        return performanceSeatWriteService.hold(performanceId, participantNo)
    }

    fun reserve(userId: Long, performanceId: Long, idempotencyKey: String): ReservationResponse =
        reservationFacade.reserve(userId, performanceId, idempotencyKey)

    fun seatState(performanceId: Long, seatId: Long): SeatState =
        seatStates(performanceId, listOf(seatId)).single()

    fun seatStates(performanceId: Long, seatIds: List<Long>): List<SeatState> =
        jdbcTemplate.query(
            "SELECT status, participant_no, expires_at FROM performance_seats WHERE performance_id = ? AND seat_id IN (${seatIds.joinToString()}) ORDER BY seat_id",
            { rs, _ ->
                SeatState(
                    rs.getString("status"),
                    rs.getObject("participant_no", Int::class.javaObjectType),
                    rs.getObject("expires_at", LocalDateTime::class.java),
                )
            },
            performanceId,
        )

    // 회차가 끝난 방식. 진행 중이면 null이다.
    fun performanceResult(performanceId: Long): String? =
        jdbcTemplate.queryForObject("SELECT result FROM performances WHERE id = ?", String::class.java, performanceId)

    fun recordResults(userId: Long): List<String> =
        jdbcTemplate.query("SELECT result FROM records WHERE user_id = ? ORDER BY id", { rs, _ -> rs.getString(1) }, userId)

    fun count(table: String, performanceId: Long): Long =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $table WHERE performance_id = ?", Long::class.java, performanceId)!!
}

data class SeatState(val status: String, val participantNo: Int?, val expiresAt: LocalDateTime?)
