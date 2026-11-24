package io.github.hyeon0208.firstcome.support.test

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@TestConfiguration(proxyBeanMethods = false)
@Import(TestFixture::class)
class TestBeanConfiguration {
    @Bean
    @Primary
    fun testClock(): TestClock = TestClock(TestClock.INITIAL_INSTANT, TestClock.ZONE)

    @Bean
    @Primary
    fun fakePaymentClient(): FakePaymentClient = FakePaymentClient()
}
