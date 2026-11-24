package io.github.hyeon0208.firstcome.support.config

import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TimeConfiguration {

    @Bean
    fun clock(): Clock =
        Clock.tick(Clock.system(ZoneId.of("Asia/Seoul")), Duration.ofNanos(1_000))
}
