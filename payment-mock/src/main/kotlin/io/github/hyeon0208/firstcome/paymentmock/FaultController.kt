package io.github.hyeon0208.firstcome.paymentmock

import java.time.Duration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

// 거절·지연·무응답 주입. 설정으로 켰을 때만 등록되고, 평소에는 이 경로가 없다(404).
@RestController
@ConditionalOnProperty("payment-mock.fault-injection.enabled", havingValue = "true")
class FaultController(
    private val faultSettings: FaultSettings,
) {
    @PutMapping("/test/faults")
    fun setFault(@RequestBody request: FaultRequest): FaultRequest {
        faultSettings.delay = Duration.ofMillis(request.delayMs)
        faultSettings.mode = request.mode
        return request
    }
}
