package io.github.hyeon0208.firstcome.support.error

import io.github.hyeon0208.firstcome.support.test.BaseControllerTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class CommonErrorResponseTest : BaseControllerTest() {
    init {
        given("공통 오류 응답 규칙(프레임워크가 처리하는 오류도 {code, message})으로") {
            `when`("없는 경로로 요청하면") {
                val result = mockMvc.get("/api/none")

                then("404 RESOURCE_NOT_FOUND를 {code, message}로 돌려준다") {
                    result.andExpect {
                        status { isNotFound() }
                        jsonPath("$.code") { value("RESOURCE_NOT_FOUND") }
                        jsonPath("$.message") { isNotEmpty() }
                    }
                }
            }

            `when`("GET만 받는 경로(/v3/api-docs)에 DELETE로 요청하면") {
                val result = mockMvc.delete("/v3/api-docs")

                then("405 METHOD_NOT_ALLOWED를 {code, message}로 돌려준다") {
                    result.andExpect {
                        status { isMethodNotAllowed() }
                        jsonPath("$.code") { value("METHOD_NOT_ALLOWED") }
                        jsonPath("$.message") { isNotEmpty() }
                    }
                }
            }

            `when`("JSON을 받는 경로(/api/v1/users)에 text/plain으로 요청하면") {
                val result = mockMvc.post("/api/v1/users") {
                    contentType = MediaType.TEXT_PLAIN
                    content = "연습생"
                }

                then("415 UNSUPPORTED_MEDIA_TYPE을 {code, message}로 돌려준다") {
                    result.andExpect {
                        status { isUnsupportedMediaType() }
                        jsonPath("$.code") { value("UNSUPPORTED_MEDIA_TYPE") }
                        jsonPath("$.message") { isNotEmpty() }
                    }
                }
            }
        }
    }
}
