package io.github.hyeon0208.firstcome.paymentmock

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("payment-mock")
data class PaymentMockProperties(
    val delayMin: Duration,
    val delayMax: Duration,
    val noResponseHold: Duration,
)
