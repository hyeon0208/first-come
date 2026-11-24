package io.github.hyeon0208.firstcome.paymentmock

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tools.jackson.databind.json.JsonMapper

@SpringBootTest(properties = ["payment-mock.delay-min=0ms", "payment-mock.delay-max=0ms"])
@AutoConfigureMockMvc
class PaymentControllerTest : BehaviorSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    private fun approve(orderId: Long): String {
        val body = mockMvc.post("/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderId": $orderId, "amount": 10000}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return jsonMapper.readTree(body).path("transactionId").asString()
    }

    private fun cancel(orderId: Long): Boolean {
        val body = mockMvc.post("/payments/cancel") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderId": $orderId}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return jsonMapper.readTree(body).path("cancelled").asBoolean()
    }

    init {
        given("모의 결제 서버에") {
            `when`("주문 1로 두 번, 주문 2로 한 번 승인을 요청하면") {
                val first = approve(1)
                val retry = approve(1)
                val other = approve(2)

                then("같은 주문이면 같은 승인 번호를, 다른 주문이면 다른 승인 번호를 돌려준다") {
                    retry shouldBe first
                    other shouldNotBe first
                }
            }

            `when`("승인된 주문 1과 승인이 없는 주문 9를 취소하면") {
                val cancelledApproved = cancel(1)
                val cancelledUnknown = cancel(9)

                then("둘 다 성공으로 답하고, 승인이 있었는지만 알려 준다") {
                    cancelledApproved shouldBe true
                    cancelledUnknown shouldBe false
                }
            }

            `when`("취소된 주문 9에 뒤늦게 승인을 요청하면") {
                val result = mockMvc.post("/payments") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"orderId": 9, "amount": 10000}"""
                }

                then("승인하지 않는다(409 CANCELLED). 늦은 승인이 취소 뒤에 남지 않는다") {
                    result.andExpect {
                        status { isConflict() }
                        jsonPath("$.code") { value("CANCELLED") }
                    }
                }
            }

            `when`("장애 주입을 켜지 않은 채 /test/faults 를 부르면") {
                val result = mockMvc.put("/test/faults") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"mode": "DECLINE"}"""
                }

                then("그 경로는 없다(404)") {
                    result.andExpect { status { isNotFound() } }
                }
            }
        }
    }
}
