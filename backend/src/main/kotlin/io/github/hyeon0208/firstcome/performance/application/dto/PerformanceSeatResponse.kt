package io.github.hyeon0208.firstcome.performance.application.dto

import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatEntity
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatStatus
import io.github.hyeon0208.firstcome.show.domain.SeatEntity
import io.github.hyeon0208.firstcome.show.domain.SeatGrade

data class PerformanceSeatResponse(
    val performanceSeatId: Long,
    val row: Int,
    val column: Int,
    val grade: SeatGrade,
    val price: Long,
    val status: PerformanceSeatStatus,
) {
    companion object {
        fun of(
            performanceSeat: PerformanceSeatEntity,
            seat: SeatEntity,
            price: Long,
            status: PerformanceSeatStatus,
        ): PerformanceSeatResponse =
            PerformanceSeatResponse(
                performanceSeatId = performanceSeat.id,
                row = seat.row,
                column = seat.column,
                grade = seat.grade,
                price = price,
                status = status,
            )
    }
}
