package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatStatus
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.github.hyeon0208.firstcome.support.test.SeatState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.LocalDateTime

class PerformanceSeatWriteServiceTest : BaseServiceTest() {
    init {
        given("사용자와 초기 데이터가 있을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            val now = LocalDateTime.now(testClock)

            `when`("1행 좌석 19,20을 임시 배정하면") {
                val response = fixture.holdSeats(userId, performanceId = 1, listOf(19, 20))

                then("응답과 DB 모두 그 사용자의 임시 배정(만료 5분 뒤)이고, 가격 합계는 2석 가격이다") {
                    response.seats.map { it.status } shouldBe List(2) { PerformanceSeatStatus.HELD }
                    response.totalPrice shouldBe 300_000L
                    response.holdExpiresAt shouldBe now.plusMinutes(5)
                    fixture.seatState(19) shouldBe SeatState("HELD", userId, now.plusMinutes(5))
                    fixture.seatState(20) shouldBe SeatState("HELD", userId, now.plusMinutes(5))
                }
            }
        }

        given("사용자가 좌석 1,2를 임시 배정하고 1분이 지났을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            testClock.advance(Duration.ofMinutes(1))
            val now = LocalDateTime.now(testClock)

            `when`("좌석 2,3을 새로 요청하면") {
                fixture.holdSeats(userId, performanceId = 1, listOf(2, 3))

                then("기존 배정이 새 요청으로 대체된다(1은 풀리고 2,3은 새 만료 시각)") {
                    fixture.seatState(1) shouldBe SeatState("AVAILABLE", null, null)
                    fixture.seatState(2) shouldBe SeatState("HELD", userId, now.plusMinutes(5))
                    fixture.seatState(3) shouldBe SeatState("HELD", userId, now.plusMinutes(5))
                }
            }
        }

        given("사용자가 회차 1의 좌석 1을 임시 배정했을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            val now = LocalDateTime.now(testClock)
            fixture.holdSeats(userId, performanceId = 1, listOf(1))

            `when`("회차 2의 좌석 401을 임시 배정하면") {
                fixture.holdSeats(userId, performanceId = 2, listOf(401))

                then("다른 회차의 배정은 대체하지 않아 두 회차의 임시 배정이 모두 남는다") {
                    fixture.seatStates(listOf(1, 401)) shouldBe List(2) { SeatState("HELD", userId, now.plusMinutes(5)) }
                }
            }
        }

        given("다른 사용자가 좌석 1을, 내가 좌석 5를 임시 배정했을 때") {
            loadInitialData()
            val otherUserId = fixture.createUser()
            val userId = fixture.createUser()
            fixture.holdSeats(otherUserId, performanceId = 1, listOf(1))
            fixture.holdSeats(userId, performanceId = 1, listOf(5))

            `when`("내가 좌석 1,6을 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.holdSeats(userId, performanceId = 1, listOf(1, 6)) }

                then("SEAT_NOT_AVAILABLE로 거부되고, 내 기존 배정(5)은 그대로 남는다") {
                    exception.errorCode shouldBe PerformanceErrorCode.SEAT_NOT_AVAILABLE
                    fixture.seatState(5).holderId shouldBe userId
                    fixture.seatState(5).status shouldBe "HELD"
                    fixture.seatState(6).status shouldBe "AVAILABLE"
                }
            }
        }

        given("다른 사용자의 배정이 만료 시각과 같은 순간에") {
            loadInitialData()
            val otherUserId = fixture.createUser()
            val userId = fixture.createUser()
            fixture.holdSeats(otherUserId, performanceId = 1, listOf(1))
            testClock.advance(Duration.ofMinutes(5))

            `when`("좌석 1을 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.holdSeats(userId, performanceId = 1, listOf(1)) }

                then("아직 임시 배정 중이라 SEAT_NOT_AVAILABLE로 거부된다") {
                    exception.errorCode shouldBe PerformanceErrorCode.SEAT_NOT_AVAILABLE
                }
            }
        }

        given("다른 사용자의 배정이 만료 시각을 1초 지났을 때") {
            loadInitialData()
            val otherUserId = fixture.createUser()
            val userId = fixture.createUser()
            fixture.holdSeats(otherUserId, performanceId = 1, listOf(1))
            testClock.advance(Duration.ofMinutes(5).plusSeconds(1))

            `when`("좌석 1을 요청하면") {
                fixture.holdSeats(userId, performanceId = 1, listOf(1))

                then("내 임시 배정이 된다") {
                    fixture.seatState(1).holderId shouldBe userId
                }
            }
        }

        given("회차 1의 공연 시작 시각(2026-12-25 10:00)이 되었을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            testClock.advance(Duration.ofDays(24))

            `when`("임시 배정을 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.holdSeats(userId, performanceId = 1, listOf(1)) }

                then("PERFORMANCE_ALREADY_STARTED로 거부된다") {
                    exception.errorCode shouldBe PerformanceErrorCode.PERFORMANCE_ALREADY_STARTED
                }
            }
        }

        given("사용자의 좌석 1, 2가 결제 중일 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            fixture.createPendingReservation(userId, performanceId = 1, listOf(1, 2), "key-1")

            `when`("같은 사용자가 다른 좌석 3을 임시 배정하려 하면") {
                val exception = shouldThrow<BusinessException> { fixture.holdSeats(userId, performanceId = 1, listOf(3)) }

                then("SEATS_PAYING으로 거부되고 좌석 3은 예약 가능 그대로다") {
                    exception.errorCode shouldBe PerformanceErrorCode.SEATS_PAYING
                    fixture.seatState(3) shouldBe SeatState("AVAILABLE", null, null)
                }
            }
        }
    }
}
