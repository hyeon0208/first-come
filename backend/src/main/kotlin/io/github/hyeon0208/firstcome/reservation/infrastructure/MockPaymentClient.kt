package io.github.hyeon0208.firstcome.reservation.infrastructure

import io.github.hyeon0208.firstcome.reservation.application.PaymentApproval
import io.github.hyeon0208.firstcome.reservation.application.PaymentClient
import io.github.hyeon0208.firstcome.reservation.application.PaymentRequest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local")
class MockPaymentClient : PaymentClient {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val approvalsByReservationId = ConcurrentHashMap<Long, PaymentApproval>()

    override fun pay(request: PaymentRequest): PaymentApproval =
        approvalsByReservationId.computeIfAbsent(request.reservationId) {
            PaymentApproval(transactionId = "MOCK-${UUID.randomUUID()}").also {
                logger.info(
                    "모의 결제 승인 reservationId={} amount={} transactionId={}",
                    request.reservationId,
                    request.amount,
                    it.transactionId,
                )
            }
        }

    override fun cancel(transactionId: String) {
        logger.info("모의 결제 취소 transactionId={}", transactionId)
    }
}
