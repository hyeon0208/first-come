package io.github.hyeon0208.firstcome.reservation.infrastructure

import io.github.hyeon0208.firstcome.reservation.application.PaymentApproval
import io.github.hyeon0208.firstcome.reservation.application.PaymentClient
import io.github.hyeon0208.firstcome.reservation.application.PaymentRequest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

// shortcut: 서버 안에서 바로 승인하는 임시 구현. 별도 컨테이너의 모의 결제 서버를 부르는 구현으로 바꾼다.
@Component
class MockPaymentClient : PaymentClient {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val approvalsByReservationId = ConcurrentHashMap<Long, PaymentApproval>()

    override fun pay(request: PaymentRequest): PaymentApproval =
        approvalsByReservationId.computeIfAbsent(request.reservationId) {
            PaymentApproval(transactionId = "MOCK-${UUID.randomUUID()}").also {
                logger.info("모의 결제 승인 reservationId={} amount={} transactionId={}", request.reservationId, request.amount, it.transactionId)
            }
        }

    override fun cancel(reservationId: Long) {
        val approval = approvalsByReservationId.remove(reservationId)
        logger.info("모의 결제 취소 reservationId={} transactionId={}", reservationId, approval?.transactionId)
    }
}
