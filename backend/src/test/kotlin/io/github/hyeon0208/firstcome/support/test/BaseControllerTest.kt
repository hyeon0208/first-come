package io.github.hyeon0208.firstcome.support.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestBeanConfiguration::class, MySqlContainerConfiguration::class)
abstract class BaseControllerTest : BaseSpringTest() {
    @Autowired
    protected lateinit var mockMvc: MockMvc
}
