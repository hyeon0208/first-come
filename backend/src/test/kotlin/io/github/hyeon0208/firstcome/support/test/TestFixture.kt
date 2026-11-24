package io.github.hyeon0208.firstcome.support.test

import io.github.hyeon0208.firstcome.performance.application.PerformanceSeatWriteService
import io.github.hyeon0208.firstcome.performance.application.dto.HoldSeatsRequest
import io.github.hyeon0208.firstcome.performance.application.dto.HoldSeatsResponse
import io.github.hyeon0208.firstcome.reservation.application.ReservationFacade
import io.github.hyeon0208.firstcome.reservation.application.ReservationWriteService
import io.github.hyeon0208.firstcome.reservation.application.dto.CreateReservationRequest
import io.github.hyeon0208.firstcome.reservation.application.dto.PendingReservation
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationStart
import io.github.hyeon0208.firstcome.user.domain.UserEntity
import io.github.hyeon0208.firstcome.user.infrastructure.JpaUserRepository
import java.time.LocalDateTime
import org.springframework.jdbc.core.JdbcTemplate

class TestFixture(
    private val userRepository: JpaUserRepository,
    private val performanceSeatWriteService: PerformanceSeatWriteService,
    private val reservationWriteService: ReservationWriteService,
    private val reservationFacade: ReservationFacade,
    private val jdbcTemplate: JdbcTemplate,
) {
    fun createUser(): Long =
        userRepository.save(UserEntity(name = "홍길동")).id

    fun holdSeats(userId: Long, performanceId: Long, performanceSeatIds: List<Long>): HoldSeatsResponse =
        performanceSeatWriteService.holdSeats(
            venueId = 1,
            showId = 1,
            performanceId = performanceId,
            request = HoldSeatsRequest(userId = userId, performanceSeatIds = performanceSeatIds),
        )

    fun createPendingReservation(
        userId: Long,
        performanceId: Long,
        performanceSeatIds: List<Long>,
        idempotencyKey: String,
    ): PendingReservation {
        val started = reservationWriteService.startReservation(
            request = CreateReservationRequest(
                userId = userId,
                performanceId = performanceId,
                performanceSeatIds = performanceSeatIds,
            ),
            idempotencyKey = idempotencyKey,
        )
        return (started as ReservationStart.Pending).pending
    }

    fun createReservation(
        userId: Long,
        performanceId: Long,
        performanceSeatIds: List<Long>,
        idempotencyKey: String,
    ): ReservationResponse =
        reservationFacade.createReservation(
            request = CreateReservationRequest(
                userId = userId,
                performanceId = performanceId,
                performanceSeatIds = performanceSeatIds,
            ),
            idempotencyKey = idempotencyKey,
        )

    fun seatState(performanceSeatId: Long): SeatState =
        seatStates(listOf(performanceSeatId)).single()

    fun seatStates(performanceSeatIds: List<Long>): List<SeatState> =
        jdbcTemplate.query(
            "SELECT status, holder_id, hold_expires_at FROM performance_seats WHERE id IN (${performanceSeatIds.joinToString()}) ORDER BY id",
        ) { rs, _ ->
            SeatState(
                rs.getString("status"),
                rs.getObject("holder_id", Long::class.javaObjectType),
                rs.getObject("hold_expires_at", LocalDateTime::class.java),
            )
        }
}

data class SeatState(val status: String, val holderId: Long?, val holdExpiresAt: LocalDateTime?)
