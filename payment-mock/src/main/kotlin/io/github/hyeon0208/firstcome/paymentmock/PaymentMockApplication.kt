package io.github.hyeon0208.firstcome.paymentmock

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

// 실제 결제사 대신 쓰는 모의 결제 서버. 승인과 취소만 하고, 승인 내역은 메모리에 둔다.
@SpringBootApplication
@ConfigurationPropertiesScan
class PaymentMockApplication

fun main(args: Array<String>) {
    runApplication<PaymentMockApplication>(*args)
}
