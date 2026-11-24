package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Duration
import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

// 회차의 생애: 사용자별로 만들고, 끝나면 결과만 남기고, 데이터는 지운다.
class PerformanceLifecycleTest : BaseServiceTest() {
    @Autowired
    private lateinit var performanceWriteService: PerformanceWriteService

    @Autowired
    private lateinit var bookingFacade: BookingFacade

    @Autowired
    private lateinit var performanceRepository: JpaPerformanceRepository

    @Autowired
    private lateinit var bookingProperties: BookingProperties

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private fun elapsedSeconds(userId: Long): List<Long> =
        jdbcTemplate.query("SELECT elapsed_seconds FROM records WHERE user_id = ? ORDER BY id", { rs, _ -> rs.getLong(1) }, userId)

    init {
        given("사용자가 연습을 시작했을 때") {
            val userId = fixture.createUser()
            val createdAt = LocalDateTime.now(testClock)
            val performanceId = fixture.createPerformance(userId)

            `when`("회차를 보면") {
                val performance = performanceWriteService.findCurrentPerformance(userId)!!

                then("예매 오픈은 10초 뒤, 공연 시작은 30일 뒤 18:00이고, 그 사용자만의 좌석 1,008석이 만들어진다") {
                    performance.openAt shouldBe createdAt.plusSeconds(10)
                    performance.startAt shouldBe createdAt.toLocalDate().plusDays(30).atTime(18, 0)
                    fixture.count("performance_seats", performanceId) shouldBe 1_008L
                }
            }

            `when`("연습을 하나 더 시작하려 하면") {
                val exception = shouldThrow<BusinessException> { fixture.createPerformance(userId) }

                then("진행 중인 회차가 있어 ALREADY_IN_PROGRESS로 거절된다") {
                    exception.errorCode shouldBe PerformanceErrorCode.ALREADY_IN_PROGRESS
                }
            }

            `when`("다른 사용자가 연습을 시작하면") {
                val otherPerformanceId = fixture.createPerformance(fixture.createUser())

                then("좌석을 나누지 않는 다른 회차가 만들어진다") {
                    otherPerformanceId shouldNotBe performanceId
                    fixture.count("performance_seats", otherPerformanceId) shouldBe 1_008L
                }
            }

            `when`("오픈 전에 처음부터 다시를 누르면") {
                performanceWriteService.abandon(userId)

                then("중단으로 끝나고 걸린 시간은 0초다") {
                    fixture.recordResults(userId) shouldBe listOf("ABANDONED")
                    elapsedSeconds(userId) shouldBe listOf(0L)
                }
                then("새 회차를 만들 수 있고, 그때 끝난 회차의 좌석 데이터는 지워지며 연습 기록은 남는다") {
                    val newPerformanceId = fixture.createPerformance(userId)
                    newPerformanceId shouldNotBe performanceId
                    fixture.count("performance_seats", performanceId) shouldBe 0L
                    fixture.recordResults(userId) shouldBe listOf("ABANDONED")
                }
            }

        }

        given("사용자가 연습을 시작하고 예매 화면에 들어오지 않았을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.createPerformance(userId)

            `when`("오픈 뒤 10분이 되기 직전에 정리 작업이 돌면") {
                testClock.advance(Duration.ofSeconds(10).plusMinutes(10))
                performanceWriteService.endIfDue(performanceId)

                then("아직 끝내지 않는다") {
                    fixture.performanceResult(performanceId) shouldBe null
                }
            }

            `when`("오픈 뒤 10분이 지나고 정리 작업이 돌면") {
                testClock.advance(Duration.ofSeconds(1))
                performanceWriteService.endIfDue(performanceId)

                then("중단으로 끝낸다") {
                    fixture.recordResults(userId) shouldBe listOf("ABANDONED")
                }
            }
        }

        given("사용자가 오픈 72초 뒤에 회차를 중단했을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            testClock.advance(Duration.ofSeconds(72))
            performanceWriteService.abandon(userId)

            `when`("연습 기록을 보면") {
                then("걸린 시간은 오픈 시각부터 끝난 순간까지인 72초다") {
                    elapsedSeconds(userId) shouldBe listOf(72L)
                }
            }

            `when`("끝난 회차의 화면에서 요청이 오면") {
                val exception = shouldThrow<BusinessException> { bookingFacade.getState(userId, performanceId) }

                then("SESSION_EXPIRED로 거절된다") {
                    exception.errorCode shouldBe PerformanceErrorCode.SESSION_EXPIRED
                }
            }

            `when`("다른 사용자가 그 회차 번호로 요청하면") {
                val otherUserId = fixture.createUser()
                val exception = shouldThrow<BusinessException> { bookingFacade.getState(otherUserId, performanceId) }

                then("남의 회차라 SESSION_EXPIRED로 거절된다") {
                    exception.errorCode shouldBe PerformanceErrorCode.SESSION_EXPIRED
                }
            }

            `when`("회차 데이터를 지우면") {
                performanceWriteService.purge(performanceId)

                then("좌석과 참가자, 회차가 지워지고 연습 기록만 남는다") {
                    fixture.count("performance_seats", performanceId) shouldBe 0L
                    fixture.count("performance_participants", performanceId) shouldBe 0L
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM performances WHERE id = ?", Long::class.java, performanceId) shouldBe 0L
                    fixture.recordResults(userId) shouldBe listOf("ABANDONED")
                }
            }
        }

        given("동시에 진행되는 회차가 상한(10개)에 닿았을 때") {
            repeat(10) { fixture.createPerformance(fixture.createUser()) }

            `when`("11번째 사용자가 연습을 시작하려 하면") {
                val exception = shouldThrow<BusinessException> { fixture.createPerformance(fixture.createUser()) }

                then("PRACTICE_FULL로 거절된다") {
                    exception.errorCode shouldBe PerformanceErrorCode.PRACTICE_FULL
                }
            }
        }

        given("서버가 멈춰 진행 중인 회차가 남았을 때") {
            val userId = fixture.createUser()
            fixture.enterPerformance(userId)

            `when`("서버가 다시 떠서 진행 중이던 회차를 정리하면") {
                performanceWriteService.endAllActive()

                then("중단으로 끝나고 사용자는 새 연습을 시작할 수 있다") {
                    fixture.recordResults(userId) shouldBe listOf("ABANDONED")
                    fixture.createPerformance(userId)
                }
            }
        }
        given("회차 하나는 11분 전에 끝났고, 다른 하나는 9분 전에 끝났을 때") {
            val oldUserId = fixture.createUser()
            val oldPerformanceId = fixture.createPerformance(oldUserId)
            performanceWriteService.abandon(oldUserId)
            testClock.advance(Duration.ofMinutes(2))
            val recentUserId = fixture.createUser()
            val recentPerformanceId = fixture.createPerformance(recentUserId)
            performanceWriteService.abandon(recentUserId)
            testClock.advance(Duration.ofMinutes(9))

            `when`("정리 작업이 돌면") {
                PerformanceSweeper(performanceRepository, performanceWriteService, bookingProperties, testClock).sweep()

                then("끝난 지 10분이 지난 회차의 좌석 데이터만 지운다") {
                    fixture.count("performance_seats", oldPerformanceId) shouldBe 0L
                    fixture.count("performance_seats", recentPerformanceId) shouldBe 1_008L
                }
                then("연습 기록은 둘 다 남는다") {
                    fixture.recordResults(oldUserId) shouldBe listOf("ABANDONED")
                    fixture.recordResults(recentUserId) shouldBe listOf("ABANDONED")
                }
            }
        }
    }
}
