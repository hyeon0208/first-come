package io.github.hyeon0208.firstcome.performance.application.dto

import io.github.hyeon0208.firstcome.performance.BookingPhase
import java.time.LocalDateTime

// 한 참가자가 지금 가진 좌석과 단계. expired는 선점한 좌석의 예매 가능 시간이 지났다는 뜻이다.
data class ParticipantSeatsResponse(
    val phase: BookingPhase,
    val seatIds: List<Long>,
    val expiresAt: LocalDateTime?,
    val expired: Boolean,
)
