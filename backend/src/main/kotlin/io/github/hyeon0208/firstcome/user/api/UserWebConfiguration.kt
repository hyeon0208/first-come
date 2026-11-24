package io.github.hyeon0208.firstcome.user.api

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class UserWebConfiguration(
    private val sessionInterceptor: SessionInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(sessionInterceptor).addPathPatterns("/api/**")
    }
}
