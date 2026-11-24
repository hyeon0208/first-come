package io.github.hyeon0208.firstcome.performance.application.dto

import io.github.hyeon0208.firstcome.performance.Difficulty
import io.github.hyeon0208.firstcome.performance.domain.PerformanceEntity
import java.time.LocalDateTime

// serverNow는 화면이 서버 시각과의 차이를 보정해 남은 시간을 세는 데 쓴다.
data class PerformanceResponse(
    val id: Long,
    val showId: Long,
    val difficulty: Difficulty,
    val openAt: LocalDateTime,
    val startAt: LocalDateTime,
    val enteredAt: LocalDateTime?,
    val seatSelectionExpiresAt: LocalDateTime?,
    val active: Boolean,
    val serverNow: LocalDateTime,
) {
    companion object {
        fun of(performance: PerformanceEntity, now: LocalDateTime): PerformanceResponse =
            PerformanceResponse(
                id = performance.id,
                showId = performance.showId,
                difficulty = performance.difficulty,
                openAt = performance.openAt,
                startAt = performance.startAt,
                enteredAt = performance.enteredAt,
                seatSelectionExpiresAt = performance.seatSelectionExpiresAt,
                active = performance.isActive,
                serverNow = now,
            )
    }
}
