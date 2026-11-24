package io.github.hyeon0208.firstcome.reservation.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("reservation")
data class ReservationProperties(
    // 장당 예매수수료
    val feePerTicket: Long,
)
