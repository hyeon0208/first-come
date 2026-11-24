package io.github.hyeon0208.firstcome.support.config

import io.swagger.v3.oas.models.media.StringSchema
import io.github.hyeon0208.firstcome.support.web.CurrentUserId
import java.time.LocalDateTime
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {

    init {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUserId::class.java)
        SpringDocUtils.getConfig().replaceWithSchema(
            LocalDateTime::class.java,
            StringSchema()
                .pattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,6})?$")
                .example("2026-11-12T10:15:30.123456")
                .description("한국 시간 ISO 8601 날짜·시각(시간대 표기 없음). 소수점 이하는 마이크로초까지이고 0이면 생략"),
        )
    }
}
