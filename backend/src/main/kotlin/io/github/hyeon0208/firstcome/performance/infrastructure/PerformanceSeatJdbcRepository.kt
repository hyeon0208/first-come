package io.github.hyeon0208.firstcome.performance.infrastructure

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

// 회차를 만들 때 좌석 수만큼(1,008행) 회차 좌석을 넣는다. 엔티티를 하나씩 저장하지 않고 한 번에 묶어 보낸다.
@Repository
class PerformanceSeatJdbcRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun insertAvailableSeats(performanceId: Long, seatIds: List<Long>) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO performance_seats (performance_id, seat_id, status) VALUES (?, ?, 'AVAILABLE')",
            seatIds,
            seatIds.size,
        ) { statement, seatId ->
            statement.setLong(1, performanceId)
            statement.setLong(2, seatId)
        }
    }
}
