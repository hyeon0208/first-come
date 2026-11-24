package io.github.hyeon0208.firstcome.reservation.infrastructure

import io.github.hyeon0208.firstcome.reservation.application.PaymentRequest
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MockPaymentClientTest : BehaviorSpec({
    given("모의 결제가 있을 때") {
        val paymentClient = MockPaymentClient()

        `when`("예약 1로 두 번, 예약 2로 한 번 결제하면") {
            val first = paymentClient.pay(PaymentRequest(reservationId = 1, amount = 10_000))
            val retry = paymentClient.pay(PaymentRequest(reservationId = 1, amount = 10_000))
            val other = paymentClient.pay(PaymentRequest(reservationId = 2, amount = 10_000))

            then("같은 예약이면 승인을 새로 만들지 않고 같은 승인 번호를 돌려준다") {
                retry.transactionId shouldBe first.transactionId
            }
            then("다른 예약이면 다른 승인 번호를 돌려준다") {
                other.transactionId shouldNotBe first.transactionId
            }
        }
    }
})
