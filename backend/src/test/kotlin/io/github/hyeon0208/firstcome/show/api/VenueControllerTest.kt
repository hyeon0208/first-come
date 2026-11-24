package io.github.hyeon0208.firstcome.show.api

import io.github.hyeon0208.firstcome.support.test.BaseControllerTest
import org.springframework.test.web.servlet.get

class VenueControllerTest : BaseControllerTest() {
    init {
        given("초기 데이터가 있을 때") {
            loadInitialData()

            `when`("공연장 목록을 요청하면") {
                val result = mockMvc.get("/api/venues")

                then("공연장 id와 이름을 돌려준다") {
                    result.andExpect {
                        status { isOk() }
                        jsonPath("$.length()") { value(1) }
                        jsonPath("$[0].id") { value(1) }
                        jsonPath("$[0].name") { value("연습장 홀") }
                    }
                }
            }
        }
    }
}
