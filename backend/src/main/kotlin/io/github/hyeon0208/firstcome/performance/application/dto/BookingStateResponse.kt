package io.github.hyeon0208.firstcome.performance.application.dto

import io.github.hyeon0208.firstcome.performance.BookingPhase
import java.time.LocalDateTime

// 예매 화면이 타이머와 내 좌석을 그리는 데 필요한 값. 세 시각은 서버가 정하고 화면은 serverNow와의 차이로 남은 시간을 센다.
data class BookingStateResponse(
    val performanceId: Long,
    val showId: Long,
    val phase: BookingPhase,
    val mySeatIds: List<Long>,
    val seatSelectionExpiresAt: LocalDateTime?,
    val preselectExpiresAt: LocalDateTime?,
    val holdExpiresAt: LocalDateTime?,
    val serverNow: LocalDateTime,
) {
    companion object {
        fun of(performance: PerformanceResponse, seats: ParticipantSeatsResponse, preselectExpiresAt: LocalDateTime?): BookingStateResponse =
            BookingStateResponse(
                performanceId = performance.id,
                showId = performance.showId,
                phase = seats.phase,
                mySeatIds = seats.seatIds,
                seatSelectionExpiresAt = performance.seatSelectionExpiresAt,
                preselectExpiresAt = preselectExpiresAt.takeIf { seats.phase == BookingPhase.SELECTING },
                holdExpiresAt = seats.expiresAt.takeIf { seats.phase != BookingPhase.SELECTING },
                serverNow = performance.serverNow,
            )
    }
}
