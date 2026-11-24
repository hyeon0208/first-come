package io.github.hyeon0208.firstcome.paymentmock

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PaymentMockApplication

fun main(args: Array<String>) {
    runApplication<PaymentMockApplication>(*args)
}
