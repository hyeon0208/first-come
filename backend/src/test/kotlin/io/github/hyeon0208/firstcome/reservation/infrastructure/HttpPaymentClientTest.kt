package io.github.hyeon0208.firstcome.reservation.infrastructure

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.hyeon0208.firstcome.reservation.application.PaymentDeclinedException
import io.github.hyeon0208.firstcome.reservation.application.PaymentRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClient

// 결제 서버와 주고받는 약속(경로, 본문, 상태 코드)을 작은 HTTP 서버로 흉내 내 확인한다.
// 실제 모의 결제 서버와 붙는 것은 docker compose 환경의 처음부터 끝까지 테스트가 확인한다.
class HttpPaymentClientTest : BehaviorSpec({
    val receivedBodies = CopyOnWriteArrayList<String>()
    var approveHandler: (HttpExchange) -> Unit = {}

    fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/payments/cancel") { exchange ->
            receivedBodies += "cancel " + exchange.requestBody.readAllBytes().decodeToString()
            respond(exchange, 200, """{"cancelled": true}""")
        }
        createContext("/payments") { exchange ->
            receivedBodies += "approve " + exchange.requestBody.readAllBytes().decodeToString()
            approveHandler(exchange)
        }
        start()
    }
    afterSpec { server.stop(0) }

    val requestFactory = SimpleClientHttpRequestFactory().apply { setReadTimeout(Duration.ofMillis(300)) }
    val client = HttpPaymentClient(RestClient.builder().requestFactory(requestFactory), "http://127.0.0.1:${server.address.port}")

    given("결제 서버가 승인할 때") {
        approveHandler = { respond(it, 200, """{"transactionId": "PM-1"}""") }

        `when`("예매 7의 268,000원 결제를 요청하면") {
            val approval = client.pay(PaymentRequest(reservationId = 7, amount = 268_000))

            then("예매 번호를 주문 번호로 보내고 승인 번호를 돌려받는다") {
                approval.transactionId shouldBe "PM-1"
                receivedBodies.last() shouldContain "\"orderId\":7"
                receivedBodies.last() shouldContain "\"amount\":268000"
            }
        }

        `when`("예매 7의 결제를 취소하면") {
            client.cancel(reservationId = 7)

            then("예매 번호로 취소를 보낸다") {
                receivedBodies.last() shouldBe """cancel {"orderId":7}"""
            }
        }
    }

    given("결제 서버가 402로 거절할 때") {
        approveHandler = { respond(it, 402, """{"code": "DECLINED", "message": "결제가 거절되었습니다."}""") }

        `when`("결제를 요청하면") {
            then("거절 예외로 바꿔 던진다") {
                shouldThrow<PaymentDeclinedException> { client.pay(PaymentRequest(reservationId = 8, amount = 1_000)) }
            }
        }
    }

    given("결제 서버가 응답 대기 시간(0.3초) 안에 답하지 않을 때") {
        approveHandler = {
            Thread.sleep(1_000)
            respond(it, 200, """{"transactionId": "PM-LATE"}""")
        }

        `when`("결제를 요청하면") {
            then("기다리다 끊고, 거절이 아닌 예외를 던진다(결과를 모르는 상태)") {
                shouldThrow<RestClientException> { client.pay(PaymentRequest(reservationId = 9, amount = 1_000)) }
            }
        }
    }
})
