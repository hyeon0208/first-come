package io.github.hyeon0208.firstcome.performance.application

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

// 예매 규칙 값. 뜻과 값은 application.yml의 booking 항목에 있다.
@ConfigurationProperties("booking")
data class BookingProperties(
    val seatSelectionDuration: Duration,
    val preselectDuration: Duration,
    val holdDuration: Duration,
    val maxSeats: Int,
    val openDelay: Duration,
    val showStartDays: Long,
    val enterGrace: Duration,
    val payingGrace: Duration,
    val purgeDelay: Duration,
    val maxActivePerformances: Long,
)
