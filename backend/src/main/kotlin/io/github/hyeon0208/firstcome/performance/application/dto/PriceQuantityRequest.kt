package io.github.hyeon0208.firstcome.performance.application.dto

import io.github.hyeon0208.firstcome.show.SeatGrade

data class PriceQuantityRequest(
    val grade: SeatGrade,
    val quantity: Int,
)
