package io.github.hyeon0208.firstcome.api

import io.github.hyeon0208.firstcome.support.test.BaseControllerTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.servlet.http.Cookie
import java.time.Duration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.json.JsonMapper

// 닉네임 입력부터 내 예매 확인까지, 그리고 다시 시작하면 새 회차로 처음부터 되는지를 HTTP로 확인한다.
class BookingFlowApiTest : BaseControllerTest() {
    @Autowired
    private lateinit var jsonMapper: JsonMapper

    private fun signUp(): Cookie =
        mockMvc.post("/api/v1/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"nickname": "연습생"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.getCookie("fc_session")!!

    private fun startPractice(session: Cookie): Long {
        val body = mockMvc.post("/api/v1/performances") {
            cookie(session)
            contentType = MediaType.APPLICATION_JSON
            content = """{"showId": 1, "difficulty": "EASY"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return jsonMapper.readTree(body).path("id").asLong()
    }

    init {
        given("처음 온 사용자가") {
            `when`("닉네임을 넣으면") {
                val response = mockMvc.post("/api/v1/users") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"nickname": "연습생"}"""
                }.andReturn().response

                then("사용자가 만들어지고 스크립트가 읽을 수 없는 세션 쿠키를 받는다") {
                    response.status shouldBe 201
                    val cookie = response.getCookie("fc_session")!!
                    cookie.isHttpOnly shouldBe true
                    cookie.value.length shouldBe 64
                    response.contentAsString.contains(cookie.value) shouldBe false
                }
            }

            `when`("닉네임 없이(쿠키 없이) 연습을 시작하려 하면") {
                val result = mockMvc.post("/api/v1/performances") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"showId": 1, "difficulty": "EASY"}"""
                }

                then("401 UNAUTHENTICATED로 거절된다") {
                    result.andExpect {
                        status { isUnauthorized() }
                        jsonPath("$.code") { value("UNAUTHENTICATED") }
                    }
                }
            }

            `when`("난이도 목록을 보면") {
                val result = mockMvc.get("/api/v1/difficulties")

                then("세 난이도의 가상 경쟁자 수와 매진까지의 시간이 나온다(쉬움은 매진되지 않는다)") {
                    result.andExpect {
                        jsonPath("$[*].difficulty") { value(org.hamcrest.Matchers.contains("EASY", "NORMAL", "HARD")) }
                        jsonPath("$[*].competitorCount") { value(org.hamcrest.Matchers.contains(200, 500, 1000)) }
                        jsonPath("$[0].soldOutSeconds") { value(null) }
                        jsonPath("$[2].soldOutSeconds") { value(10) }
                    }
                }
            }

            `when`("1자 닉네임을 넣으면") {
                val result = mockMvc.post("/api/v1/users") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"nickname": "가"}"""
                }

                then("400 INVALID_REQUEST로 거절된다") {
                    result.andExpect {
                        status { isBadRequest() }
                        jsonPath("$.code") { value("INVALID_REQUEST") }
                        jsonPath("$.message") { value("닉네임은 2~12자로 입력해 주세요.") }
                    }
                }
            }
        }

        given("닉네임을 넣은 사용자가 연습을 시작했을 때") {
            val session = signUp()
            val performanceId = startPractice(session)

            `when`("오픈 전에 예매 화면에 들어오려 하면") {
                val result = mockMvc.post("/api/v1/booking/enter") {
                    cookie(session)
                    header("X-Practice-Run", performanceId)
                }

                then("409 NOT_OPEN_YET으로 거절된다") {
                    result.andExpect {
                        status { isConflict() }
                        jsonPath("$.code") { value("NOT_OPEN_YET") }
                    }
                }
            }

            `when`("오픈 뒤 좌석 선택부터 결제까지 한 번에 진행하면") {
                testClock.advance(Duration.ofSeconds(10))
                val run: org.springframework.test.web.servlet.MockHttpServletRequestDsl.() -> Unit = {
                    cookie(session)
                    header("X-Practice-Run", performanceId)
                }
                mockMvc.get("/api/v1/shows").andExpect { jsonPath("$.length()") { value(3) } }
                mockMvc.get("/api/v1/shows/1/seat-map").andExpect { jsonPath("$.seats.length()") { value(1008) } }
                mockMvc.post("/api/v1/booking/enter", dsl = run).andExpect { status { isOk() } }
                mockMvc.get("/api/v1/booking/seats", dsl = run).andExpect {
                    jsonPath("$.unavailableSeatIds.length()") { value(0) }
                    jsonPath("$.soldOut") { value(false) }
                }
                mockMvc.post("/api/v1/booking/seats/1/preselect", dsl = run).andExpect { jsonPath("$.mySeatIds[0]") { value(1) } }
                mockMvc.post("/api/v1/booking/seats/2/preselect", dsl = run).andExpect { status { isOk() } }
                mockMvc.delete("/api/v1/booking/seats/2/preselect", dsl = run).andExpect { jsonPath("$.mySeatIds.length()") { value(1) } }
                mockMvc.post("/api/v1/booking/hold", dsl = run).andExpect {
                    jsonPath("$.unselectableSeatIds.length()") { value(0) }
                    jsonPath("$.state.phase") { value("HELD") }
                }
                mockMvc.post("/api/v1/booking/prices/validate") {
                    run()
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"quantities": [{"grade": "R", "quantity": 2}]}"""
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("PRICE_COUNT_MISMATCH") }
                }
                mockMvc.post("/api/v1/booking/prices/validate") {
                    run()
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"quantities": [{"grade": "R", "quantity": 1}]}"""
                }.andExpect { status { isNoContent() } }
                mockMvc.get("/api/v1/reservations/quote", dsl = run).andExpect { jsonPath("$.amount") { value(134_000) } }
                val reserved = mockMvc.post("/api/v1/reservations") {
                    run()
                    header("Idempotency-Key", "key-1")
                }

                then("예매가 확정된다") {
                    reserved.andExpect {
                        status { isCreated() }
                        jsonPath("$.status") { value("CONFIRMED") }
                        jsonPath("$.amount") { value(134_000) }
                        jsonPath("$.seats[0].name") { value("1층 A구역 1열 1번") }
                    }
                }
                then("내 예매에 성공 한 줄이 남고, 예매 완료 화면에서 예매를 볼 수 있다") {
                    mockMvc.get("/api/v1/records") { cookie(session) }.andExpect {
                        jsonPath("$.length()") { value(1) }
                        jsonPath("$[0].result") { value("SUCCESS") }
                        jsonPath("$[0].showTitle") { value("모래시계 밴드 겨울 단독 공연 〈첫눈〉") }
                    }
                    mockMvc.get("/api/v1/reservations/by-performance/$performanceId") { cookie(session) }.andExpect {
                        status { isOk() }
                        jsonPath("$.reservationNo") { isNotEmpty() }
                    }
                }
                then("끝난 회차의 화면에서 온 요청은 410 SESSION_EXPIRED로 거절된다") {
                    mockMvc.get("/api/v1/booking/state", dsl = run).andExpect {
                        status { isGone() }
                        jsonPath("$.code") { value("SESSION_EXPIRED") }
                    }
                }
                then("다시 시작하면 새 회차가 만들어지고 좌석은 처음 상태다") {
                    mockMvc.get("/api/v1/performances/current") { cookie(session) }.andExpect { status { isNoContent() } }
                    val newPerformanceId = startPractice(session)
                    newPerformanceId shouldNotBe performanceId
                    testClock.advance(Duration.ofSeconds(10))
                    mockMvc.post("/api/v1/booking/enter") {
                        cookie(session)
                        header("X-Practice-Run", newPerformanceId)
                    }.andExpect { status { isOk() } }
                    mockMvc.get("/api/v1/booking/seats") {
                        cookie(session)
                        header("X-Practice-Run", newPerformanceId)
                    }.andExpect { jsonPath("$.unavailableSeatIds.length()") { value(0) } }
                }
            }
        }
    }
}
