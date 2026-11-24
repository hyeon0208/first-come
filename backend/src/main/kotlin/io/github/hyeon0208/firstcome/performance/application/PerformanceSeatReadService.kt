package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.BookingPhase
import io.github.hyeon0208.firstcome.performance.ParticipantNo
import io.github.hyeon0208.firstcome.performance.application.dto.BookingStateResponse
import io.github.hyeon0208.firstcome.performance.application.dto.ParticipantSeatsResponse
import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceResponse
import io.github.hyeon0208.firstcome.performance.application.dto.SeatStatusesResponse
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatEntity
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatStatus
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaParticipantRepository
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceSeatRepository
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PerformanceSeatReadService(
    private val participantRepository: JpaParticipantRepository,
    private val performanceSeatRepository: JpaPerformanceSeatRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getSeatStatuses(performanceId: Long, participantNo: Int): SeatStatusesResponse {
        val now = LocalDateTime.now(clock)
        val takenSeats = performanceSeatRepository.findAllByPerformanceId(performanceId).filter { it.isTakenAt(now) }
        val (mySeats, otherSeats) = takenSeats.partition { it.participantNo == participantNo }
        return SeatStatusesResponse(
            unavailableSeatIds = otherSeats.map { it.seatId },
            mySeatIds = mySeats.map { it.seatId },
            soldOut = performanceSeatRepository.countAvailable(performanceId, now) == 0L,
        )
    }

    @Transactional(readOnly = true)
    fun getParticipantSeats(performanceId: Long, participantNo: Int): ParticipantSeatsResponse {
        val now = LocalDateTime.now(clock)
        val seats = performanceSeatRepository.findAllByPerformanceIdAndParticipantNo(performanceId, participantNo)
        val phase = phaseOf(seats)
        // 좌석을 고르는 중에는 가선점 시간이 남은 좌석만 내 좌석이다. 그 뒤 단계의 좌석은 만료 여부를 따로 알려 준다.
        val phaseSeats = if (phase == BookingPhase.SELECTING) seats.filter { it.isTakenAt(now) } else seats.filter { it.status == statusOf(phase) }
        val expiresAt = phaseSeats.firstNotNullOfOrNull { it.expiresAt }
        return ParticipantSeatsResponse(
            phase = phase,
            seatIds = phaseSeats.map { it.seatId }.sorted(),
            expiresAt = expiresAt,
            expired = phase == BookingPhase.HELD && expiresAt?.isBefore(now) == true,
        )
    }

    @Transactional(readOnly = true)
    fun getBookingState(performance: PerformanceResponse): BookingStateResponse {
        val participant = participantRepository.findByPerformanceIdAndParticipantNo(performance.id, ParticipantNo.USER)
        return BookingStateResponse.of(
            performance = performance,
            seats = getParticipantSeats(performance.id, ParticipantNo.USER),
            preselectExpiresAt = participant?.alivePreselectExpiresAt(performance.serverNow),
        )
    }

    private fun phaseOf(seats: List<PerformanceSeatEntity>): BookingPhase {
        val statuses = seats.map { it.status }.toSet()
        return when {
            PerformanceSeatStatus.PAYING in statuses -> BookingPhase.PAYING
            PerformanceSeatStatus.RESERVED in statuses -> BookingPhase.RESERVED
            PerformanceSeatStatus.HELD in statuses -> BookingPhase.HELD
            else -> BookingPhase.SELECTING
        }
    }

    private fun statusOf(phase: BookingPhase): PerformanceSeatStatus =
        when (phase) {
            BookingPhase.SELECTING -> PerformanceSeatStatus.PRESELECTED
            BookingPhase.HELD -> PerformanceSeatStatus.HELD
            BookingPhase.PAYING -> PerformanceSeatStatus.PAYING
            BookingPhase.RESERVED -> PerformanceSeatStatus.RESERVED
        }
}
