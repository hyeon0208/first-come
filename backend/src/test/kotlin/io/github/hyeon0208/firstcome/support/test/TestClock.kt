package io.github.hyeon0208.firstcome.support.test

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class TestClock(
    private val initialInstant: Instant,
    private val zone: ZoneId,
) : Clock() {
    @Volatile
    private var instant: Instant = initialInstant

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = fixed(instant, zone)

    override fun instant(): Instant = instant

    fun advance(duration: Duration) {
        instant = instant.plus(duration)
    }

    fun reset() {
        instant = initialInstant
    }

    companion object {
        val INITIAL_INSTANT: Instant = OffsetDateTime.parse("2026-12-01T10:00:00+09:00").toInstant()
        val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
