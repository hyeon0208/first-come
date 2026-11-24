package io.github.hyeon0208.firstcome.paymentmock

import java.time.Duration
import kotlin.random.Random
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PaymentController(
    private val paymentStore: PaymentStore,
    private val faultSettings: FaultSettings,
    private val properties: PaymentMockProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // 승인. 1~3초 뒤 항상 성공한다. 같은 주문 번호로 다시 오면 같은 승인 번호를 돌려준다.
    @PostMapping("/payments")
    fun approve(@RequestBody request: ApproveRequest): ResponseEntity<Any> {
        val mode = faultSettings.mode
        if (mode == FaultMode.DECLINE) {
            logger.info("결제 거절(주입) orderId={}", request.orderId)
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(DeclinedResponse(code = "DECLINED", message = "결제가 거절되었습니다."))
        }
        Thread.sleep(if (mode == FaultMode.DELAY) faultSettings.delay else randomDelay())
        val transactionId = paymentStore.approve(request.orderId)
        logger.info("결제 승인 orderId={} amount={} transactionId={}", request.orderId, request.amount, transactionId)
        if (mode == FaultMode.NO_RESPONSE) {
            // 승인은 됐지만 응답이 돌아가지 않는 상황. 부른 쪽의 응답 대기 시간이 먼저 끝난다.
            Thread.sleep(properties.noResponseHold)
        }
        return ResponseEntity.ok(ApproveResponse(transactionId))
    }

    // 주문 번호로 취소. 승인된 결제가 없어도 성공으로 답한다.
    @PostMapping("/payments/cancel")
    fun cancel(@RequestBody request: CancelRequest): CancelResponse {
        val cancelled = paymentStore.cancel(request.orderId)
        logger.info("결제 취소 orderId={} cancelled={}", request.orderId, cancelled)
        return CancelResponse(cancelled)
    }

    private fun randomDelay(): Duration {
        val min = properties.delayMin.toMillis()
        val max = properties.delayMax.toMillis()
        return Duration.ofMillis(if (max > min) Random.nextLong(min, max + 1) else min)
    }
}
