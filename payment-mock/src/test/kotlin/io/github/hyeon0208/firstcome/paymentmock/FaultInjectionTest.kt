package io.github.hyeon0208.firstcome.paymentmock

import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest(
    properties = ["payment-mock.delay-min=0ms", "payment-mock.delay-max=0ms", "payment-mock.fault-injection.enabled=true"],
)
@AutoConfigureMockMvc
class FaultInjectionTest : BehaviorSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    init {
        given("장애 주입을 켠 모의 결제 서버에") {
            `when`("거절을 주입하고 승인을 요청하면") {
                mockMvc.put("/test/faults") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"mode": "DECLINE"}"""
                }.andExpect { status { isOk() } }
                val result = mockMvc.post("/payments") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"orderId": 1, "amount": 10000}"""
                }

                then("402 DECLINED로 거절한다") {
                    result.andExpect {
                        status { isPaymentRequired() }
                        jsonPath("$.code") { value("DECLINED") }
                    }
                }
            }

            `when`("주입을 되돌리고 승인을 요청하면") {
                mockMvc.put("/test/faults") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"mode": "NONE"}"""
                }
                val result = mockMvc.post("/payments") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"orderId": 1, "amount": 10000}"""
                }

                then("다시 승인한다") {
                    result.andExpect { status { isOk() } }
                }
            }
        }
    }
}
