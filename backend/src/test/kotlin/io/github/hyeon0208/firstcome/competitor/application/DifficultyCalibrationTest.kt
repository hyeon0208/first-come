package io.github.hyeon0208.firstcome.competitor.application

import io.github.hyeon0208.firstcome.performance.Difficulty
import io.github.hyeon0208.firstcome.performance.PerformanceResult
import io.github.hyeon0208.firstcome.performance.application.PerformanceWriteService
import io.github.hyeon0208.firstcome.support.test.BaseSpringTest
import io.github.hyeon0208.firstcome.support.test.MySqlContainerConfiguration
import io.github.hyeon0208.firstcome.support.test.TestBeanConfiguration
import io.kotest.core.annotation.Tags
import io.kotest.matchers.doubles.shouldBeBetween
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

// 난이도 숫자를 맞추는 측정. 설정된 인원과 간격 그대로 가상 경쟁자만 돌려 매진까지 걸린 실제 시간을 잰다.
// 몇 분이 걸리므로 평소에는 돌지 않는다: ./gradlew :backend:test -Pslow
@Tags("slow")
@SpringBootTest(properties = ["competitor.enabled=true", "booking.open-delay=0s"])
@ActiveProfiles("test")
@Import(TestBeanConfiguration::class, MySqlContainerConfiguration::class)
class DifficultyCalibrationTest : BaseSpringTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var performanceWriteService: PerformanceWriteService

    private fun availableSeatCount(performanceId: Long): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM performance_seats WHERE performance_id = ? AND status = 'AVAILABLE'", Int::class.java, performanceId)!!

    // 매진(선택 가능 좌석 0)까지 걸린 초. 제한 시간 안에 매진되지 않으면 null.
    private fun secondsUntilSoldOut(difficulty: Difficulty, limitSeconds: Int): Double? {
        val userId = fixture.createUser()
        val performanceId = fixture.createPerformance(userId, difficulty = difficulty)
        val startedAt = System.nanoTime()
        try {
            return measure(performanceId, difficulty, startedAt, limitSeconds)
        } finally {
            // 아직 들어오지 않은 가상 경쟁자를 멈춘다.
            performanceWriteService.end(performanceId, PerformanceResult.ABANDONED)
        }
    }

    private fun measure(performanceId: Long, difficulty: Difficulty, startedAt: Long, limitSeconds: Int): Double? {
        while ((System.nanoTime() - startedAt) / 1e9 < limitSeconds) {
            if (availableSeatCount(performanceId) == 0) {
                return ((System.nanoTime() - startedAt) / 1e9).also { println("[난이도 측정] $difficulty 매진까지 %.1f초".format(it)) }
            }
            Thread.sleep(100)
        }
        println("[난이도 측정] $difficulty ${limitSeconds}초 안에 매진되지 않음. 남은 좌석 ${availableSeatCount(performanceId)}")
        return null
    }

    init {
        given("어려움 난이도(1,000명이 25초에 걸쳐)로") {
            `when`("가상 경쟁자만 돌리면") {
                val seconds = secondsUntilSoldOut(Difficulty.HARD, limitSeconds = 40)

                then("약 10초(8~15초)에 매진된다") {
                    (seconds != null) shouldBe true
                    seconds!!.shouldBeBetween(8.0, 15.0, 0.0)
                }
            }
        }

        given("보통 난이도(500명이 2분에 걸쳐)로") {
            `when`("가상 경쟁자만 돌리면") {
                val seconds = secondsUntilSoldOut(Difficulty.NORMAL, limitSeconds = 130)

                then("약 1분 40초(70~110초)에 매진된다") {
                    (seconds != null) shouldBe true
                    seconds!!.shouldBeBetween(70.0, 110.0, 0.0)
                }
            }
        }
    }
}
