package io.github.hyeon0208.firstcome.show.api

import io.github.hyeon0208.firstcome.support.test.BaseControllerTest
import org.hamcrest.Matchers.contains
import org.springframework.test.web.servlet.get

class ShowControllerTest : BaseControllerTest() {
    init {
        given("초기 데이터가 있을 때") {
            loadInitialData()

            `when`("공연장 1의 공연 목록을 요청하면") {
                val result = mockMvc.get("/api/venues/1/shows")
                then("공연마다 등급별 가격과 회차(시작 시각)를 돌려준다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.length()") { value(2) }
                        jsonPath("$[0].id") { value(1) }
                        jsonPath("$[0].title") { value("모래시계 밴드 겨울 단독 공연") }
                        jsonPath("$[0].seatPrices[*].grade") { value(contains("VIP", "R", "S", "A")) }
                        jsonPath("$[0].seatPrices[*].price") { value(contains(150000, 120000, 90000, 60000)) }
                        jsonPath("$[0].performances[*].id") { value(contains(1, 2, 3)) }
                        jsonPath("$[0].performances[0].startAt") { value("2026-12-25T10:00:00") }
                        jsonPath("$[1].title") { value("유리온실 3집 발매 기념 콘서트") }
                    }
                }
            }

            `when`("없는 공연장(999)의 공연 목록을 요청하면") {
                val result = mockMvc.get("/api/venues/999/shows")

                then("404 VENUE_NOT_FOUND를 돌려준다") {
                    result.andExpect {
                        status { isNotFound() }
                        jsonPath("$.code") { value("VENUE_NOT_FOUND") }
                    }
                }
            }
        }
    }
}
