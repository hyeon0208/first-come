package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.BookingPhase
import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.github.hyeon0208.firstcome.support.test.SeatState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

// 예매 규칙 표의 시간과 매수 규칙을 시계를 옮겨 가며 확인한다.
class BookingRuleTest : BaseServiceTest() {
    @Autowired
    private lateinit var bookingFacade: BookingFacade

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private fun now(): LocalDateTime = LocalDateTime.now(testClock)

    private fun errorCodeOf(block: () -> Unit) = shouldThrow<BusinessException>(block).errorCode

    init {
        given("사용자가 연습을 시작했고 예매 오픈까지 10초가 남았을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.createPerformance(userId)

            `when`("오픈 1초 전에 예매 화면에 들어오려 하면") {
                testClock.advance(Duration.ofSeconds(9))

                then("판매 시작 전이라 NOT_OPEN_YET으로 거절된다") {
                    errorCodeOf { bookingFacade.enter(userId, performanceId) } shouldBe PerformanceErrorCode.NOT_OPEN_YET
                }
            }

            `when`("오픈 시각에 들어오면") {
                testClock.advance(Duration.ofSeconds(1))
                val state = bookingFacade.enter(userId, performanceId)

                then("그 순간부터 좌석 선택 시간 10분이 흐른다") {
                    state.seatSelectionExpiresAt shouldBe now().plusMinutes(10)
                    state.phase shouldBe BookingPhase.SELECTING
                }
            }
        }

        given("사용자가 예매 화면에 들어와 첫 좌석을 눌렀을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            val firstPressedAt = now()
            val first = bookingFacade.preselect(userId, performanceId, 1)

            `when`("1분 뒤 좌석을 하나 더 누르면") {
                testClock.advance(Duration.ofMinutes(1))
                val second = bookingFacade.preselect(userId, performanceId, 2)

                then("가선점은 첫 좌석을 누른 순간부터 7분이고, 좌석을 더 눌러도 늘지 않는다") {
                    first.preselectExpiresAt shouldBe firstPressedAt.plusMinutes(7)
                    second.preselectExpiresAt shouldBe firstPressedAt.plusMinutes(7)
                    fixture.seatStates(performanceId, listOf(1, 2)) shouldBe List(2) { SeatState("PRESELECTED", 0, firstPressedAt.plusMinutes(7)) }
                }
            }

            `when`("전부 뺐다가 1분 뒤 다시 고르면") {
                bookingFacade.unpreselectAll(userId, performanceId)
                testClock.advance(Duration.ofMinutes(1))
                val again = bookingFacade.preselect(userId, performanceId, 3)

                then("7분을 새로 세지 않는다") {
                    again.preselectExpiresAt shouldBe firstPressedAt.plusMinutes(7)
                }
            }

            `when`("첫 좌석을 누른 지 7분이 지나면") {
                testClock.advance(Duration.ofMinutes(5).plusSeconds(1))

                then("고른 좌석(3)이 풀려 내 좌석이 없다") {
                    bookingFacade.getState(userId, performanceId).mySeatIds shouldBe emptyList()
                }
                then("선택 완료를 누르면 아무것도 선점하지 않고 풀린 좌석을 알려 준다") {
                    val held = bookingFacade.hold(userId, performanceId)
                    held.unselectableSeatIds shouldBe listOf(3L)
                    held.state.phase shouldBe BookingPhase.SELECTING
                }
                then("다른 참가자가 그 좌석을 가져갈 수 있다") {
                    fixture.holdSeatsAs(performanceId, participantNo = 1, listOf(3)).heldSeatIds shouldBe listOf(3L)
                }
                then("다시 고르는 첫 좌석부터 7분을 새로 센다") {
                    bookingFacade.preselect(userId, performanceId, 5).preselectExpiresAt shouldBe now().plusMinutes(7)
                }
            }
        }

        given("사용자가 좌석 4개를 가선점했을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            listOf(1L, 2L, 3L, 4L).forEach { bookingFacade.preselect(userId, performanceId, it) }

            `when`("5번째 좌석을 누르면") {
                then("1인 4매라 SEAT_LIMIT_EXCEEDED로 거절되고 그 좌석은 선택 가능 그대로다") {
                    errorCodeOf { bookingFacade.preselect(userId, performanceId, 5) } shouldBe PerformanceErrorCode.SEAT_LIMIT_EXCEEDED
                    fixture.seatState(performanceId, 5) shouldBe SeatState("AVAILABLE", null, null)
                }
            }

            `when`("좌석 하나를 빼고 다른 좌석을 누르면") {
                bookingFacade.unpreselect(userId, performanceId, 4)
                val state = bookingFacade.preselect(userId, performanceId, 5)

                then("다시 4개가 된다") {
                    state.mySeatIds shouldBe listOf(1L, 2L, 3L, 5L)
                }
            }

            `when`("선택 완료를 누르면") {
                val held = bookingFacade.hold(userId, performanceId)

                then("고른 좌석이 한 번에 선점되고 그 순간부터 예매 가능 시간 7분이 흐른다") {
                    held.unselectableSeatIds shouldBe emptyList()
                    held.state.phase shouldBe BookingPhase.HELD
                    held.state.holdExpiresAt shouldBe now().plusMinutes(7)
                    fixture.seatStates(performanceId, listOf(1, 2, 3, 5)) shouldBe List(4) { SeatState("HELD", 0, now().plusMinutes(7)) }
                }
                then("선점한 뒤에는 좌석을 더 누를 수 없다") {
                    errorCodeOf { bookingFacade.preselect(userId, performanceId, 6) } shouldBe PerformanceErrorCode.ALREADY_HOLDING
                }
            }
        }

        given("사용자가 입장 9분째에 좌석을 골라 선택 완료했을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            testClock.advance(Duration.ofMinutes(9))
            fixture.holdSeats(userId, performanceId, listOf(1, 2))
            val heldAt = now()

            `when`("좌석 선택 시간 10분이 끝나도") {
                testClock.advance(Duration.ofMinutes(1).plusSeconds(1))

                then("선점 중이면 회차가 끝나지 않고 예매 가능 시간 7분을 끝까지 준다") {
                    bookingFacade.getState(userId, performanceId).holdExpiresAt shouldBe heldAt.plusMinutes(7)
                    fixture.performanceResult(performanceId) shouldBe null
                }
                then("좌석 선택 단계로는 돌아갈 수 없다(BACK_BLOCKED)") {
                    errorCodeOf { bookingFacade.releaseHold(userId, performanceId) } shouldBe PerformanceErrorCode.BACK_BLOCKED
                    fixture.seatStates(performanceId, listOf(1, 2)).map { it.status } shouldBe listOf("HELD", "HELD")
                }
            }

            `when`("예매 가능 시간 7분이 끝나면") {
                testClock.advance(Duration.ofMinutes(7).plusSeconds(1))

                then("다음 요청은 TIME_OVER로 거절되고 회차는 시간 초과로 끝난다") {
                    errorCodeOf { bookingFacade.getState(userId, performanceId) } shouldBe PerformanceErrorCode.TIME_OVER
                    fixture.recordResults(userId) shouldBe listOf("TIMEOUT")
                }
            }
        }

        given("사용자가 좌석을 가선점만 하고 선택 완료를 누르지 않았을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            testClock.advance(Duration.ofMinutes(9))
            bookingFacade.preselect(userId, performanceId, 1)

            `when`("좌석 선택 시간 10분이 끝나면") {
                testClock.advance(Duration.ofMinutes(1).plusSeconds(1))

                then("가선점 7분이 남았어도 TIME_OVER로 거절되고 회차는 시간 초과로 끝난다") {
                    errorCodeOf { bookingFacade.hold(userId, performanceId) } shouldBe PerformanceErrorCode.TIME_OVER
                    fixture.performanceResult(performanceId) shouldBe "TIMEOUT"
                }
            }
        }

        given("사용자가 좌석 1, 2를 선점하고 3분이 지났을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeats(userId, performanceId, listOf(1, 2))
            testClock.advance(Duration.ofMinutes(3))

            `when`("좌석 선택 단계로 돌아가면") {
                val state = bookingFacade.releaseHold(userId, performanceId)

                then("선점한 좌석이 모두 풀린다") {
                    state.phase shouldBe BookingPhase.SELECTING
                    state.mySeatIds shouldBe emptyList()
                    fixture.seatStates(performanceId, listOf(1, 2)) shouldBe List(2) { SeatState("AVAILABLE", null, null) }
                }
                then("다시 고르는 첫 좌석부터 가선점 7분을 새로 센다") {
                    bookingFacade.preselect(userId, performanceId, 3).preselectExpiresAt shouldBe now().plusMinutes(7)
                }
            }
        }

        given("다른 참가자가 좌석 1을 먼저 가져갔을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeatsAs(performanceId, participantNo = 1, listOf(1))

            `when`("사용자가 좌석 1을 누르면") {
                then("SEAT_TAKEN으로 거절되고 좌석은 그 참가자의 것 그대로다") {
                    errorCodeOf { bookingFacade.preselect(userId, performanceId, 1) } shouldBe PerformanceErrorCode.SEAT_TAKEN
                    fixture.seatState(performanceId, 1).participantNo shouldBe 1
                }
            }

            `when`("좌석 상태를 받으면") {
                val statuses = bookingFacade.getSeatStatuses(userId, performanceId)

                then("그 좌석은 선택 불가이고 매진은 아니다") {
                    statuses.unavailableSeatIds shouldBe listOf(1L)
                    statuses.soldOut shouldBe false
                }
            }
        }

        given("다른 참가자들이 모든 좌석을 가져갔을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            jdbcTemplate.update("UPDATE performance_seats SET status = 'RESERVED', participant_no = 1 WHERE performance_id = ?", performanceId)

            `when`("사용자가 좌석을 누르면") {
                then("SOLD_OUT으로 거절되고 회차는 매진으로 끝난다") {
                    errorCodeOf { bookingFacade.preselect(userId, performanceId, 1) } shouldBe PerformanceErrorCode.SOLD_OUT
                    fixture.recordResults(userId) shouldBe listOf("SOLD_OUT")
                }
            }

        }

        given("다른 참가자들이 모든 좌석을 가져간 뒤 사용자가 좌석을 누르지 않고 있을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            jdbcTemplate.update("UPDATE performance_seats SET status = 'RESERVED', participant_no = 1 WHERE performance_id = ?", performanceId)

            `when`("좌석 상태를 새로 받으면") {
                val statuses = bookingFacade.getSeatStatuses(userId, performanceId)

                then("매진으로 답하고 회차는 매진으로 끝난다") {
                    statuses.soldOut shouldBe true
                    fixture.performanceResult(performanceId) shouldBe "SOLD_OUT"
                }
            }
        }

        given("다른 참가자들이 내 좌석 말고 모든 좌석을 가져갔을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            bookingFacade.preselect(userId, performanceId, 1)
            jdbcTemplate.update("UPDATE performance_seats SET status = 'RESERVED', participant_no = 1 WHERE performance_id = ? AND seat_id <> 1", performanceId)

            `when`("사용자가 다른 좌석을 눌러 거절당해도") {
                then("내 좌석이 있으므로 매진으로 끝내지 않고 SEAT_TAKEN으로 답한다") {
                    errorCodeOf { bookingFacade.preselect(userId, performanceId, 2) } shouldBe PerformanceErrorCode.SEAT_TAKEN
                    fixture.performanceResult(performanceId) shouldBe null
                }
            }
        }
    }
}
