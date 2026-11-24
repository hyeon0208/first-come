package io.github.hyeon0208.firstcome.reservation.infrastructure

import io.github.hyeon0208.firstcome.reservation.application.PaymentApproval
import io.github.hyeon0208.firstcome.reservation.application.PaymentClient
import io.github.hyeon0208.firstcome.reservation.application.PaymentDeclinedException
import io.github.hyeon0208.firstcome.reservation.application.PaymentRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

// 결제 서버를 HTTP로 부른다. 응답 대기 시간(spring.http.clients.read-timeout)을 넘기면 예외가 나가고,
// 그 뒤 처리(예매 번호로 취소)는 ReservationFacade가 한다.
@Component
class HttpPaymentClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${payment.base-url}") baseUrl: String,
) : PaymentClient {
    private val restClient = restClientBuilder.baseUrl(baseUrl).build()

    override fun pay(request: PaymentRequest): PaymentApproval =
        try {
            val approval = restClient.post()
                .uri("/payments")
                .body(ApproveBody(orderId = request.reservationId, amount = request.amount))
                .retrieve()
                .body<ApprovalBody>()
            PaymentApproval(transactionId = checkNotNull(approval) { "결제 승인 응답이 비어 있습니다. reservationId=${request.reservationId}" }.transactionId)
        } catch (exception: HttpClientErrorException) {
            if (exception.statusCode == HttpStatus.PAYMENT_REQUIRED) throw PaymentDeclinedException("결제가 거절되었습니다.") else throw exception
        }

    override fun cancel(reservationId: Long) {
        restClient.post()
            .uri("/payments/cancel")
            .body(CancelBody(orderId = reservationId))
            .retrieve()
            .toBodilessEntity()
    }

    private data class ApproveBody(val orderId: Long, val amount: Long)

    private data class ApprovalBody(val transactionId: String)

    private data class CancelBody(val orderId: Long)
}
