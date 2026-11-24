package io.github.hyeon0208.firstcome.paymentmock

data class FaultRequest(
    val mode: FaultMode,
    val delayMs: Long = 0,
)
