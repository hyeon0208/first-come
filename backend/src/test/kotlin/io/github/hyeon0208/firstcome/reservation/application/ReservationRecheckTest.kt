package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.reservation.domain.ReservationErrorCode
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.github.hyeon0208.firstcome.support.test.SeatState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

// 반드시 지킬 규칙 1: 결제를 부르기 전에 확인한 조건(예매 가능 시간, 공연 시작 시각)은 결제가 승인된 뒤 확정 단계에서 다시 확인한다.
// 어긋나면 확정하지 않고 결제를 자동 취소한 뒤 시간 초과로 알린다.
class ReservationRecheckTest : BaseServiceTest() {
    @Autowired
    private lateinit var reservationRepository: JpaReservationRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        given("사용자가 좌석 1, 2를 선점했고, 결제하는 동안 예매 가능 시간 7분이 끝날 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeats(userId, performanceId, listOf(1, 2))
            fakePaymentClient.beforeApprove = { testClock.advance(Duration.ofMinutes(7).plusSeconds(1)) }

            `when`("결제를 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.reserve(userId, performanceId, "key-1") }

                then("시간 초과(BOOKING_TIME_EXPIRED)로 거절한다") {
                    exception.errorCode shouldBe ReservationErrorCode.BOOKING_TIME_EXPIRED
                }
                then("예매를 확정하지 않는다") {
                    reservationRepository.findAll().single().status shouldBe ReservationStatus.FAILED
                }
                then("승인된 결제를 자동으로 취소한다") {
                    fakePaymentClient.approvedRequests.size shouldBe 1
                    fakePaymentClient.cancelledReservationIds shouldBe listOf(reservationRepository.findAll().single().id)
                }
                then("좌석은 예매 완료가 되지 않고 풀린다") {
                    fixture.seatStates(performanceId, listOf(1, 2)) shouldBe List(2) { SeatState("AVAILABLE", null, null) }
                }
                then("회차는 시간 초과로 끝난다") {
                    fixture.recordResults(userId) shouldBe listOf("TIMEOUT")
                }
            }
        }

        given("사용자가 좌석 1, 2를 선점했고, 결제하는 동안 공연 시작 시각이 지날 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeats(userId, performanceId, listOf(1, 2))
            // 화면에서는 공연 시작 시각(30일 뒤)이 지날 일이 없다. 회차의 시작 시각을 2분 뒤로 당겨 놓고 결제 중에 3분을 보낸다.
            jdbcTemplate.update("UPDATE performances SET start_at = ? WHERE id = ?", LocalDateTime.now(testClock).plusMinutes(2), performanceId)
            fakePaymentClient.beforeApprove = { testClock.advance(Duration.ofMinutes(3)) }

            `when`("결제를 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.reserve(userId, performanceId, "key-1") }

                then("예매 가능 시간은 4분 남았어도 시간 초과(BOOKING_TIME_EXPIRED)로 거절한다") {
                    exception.errorCode shouldBe ReservationErrorCode.BOOKING_TIME_EXPIRED
                }
                then("예매를 확정하지 않는다") {
                    reservationRepository.findAll().single().status shouldBe ReservationStatus.FAILED
                }
                then("승인된 결제를 자동으로 취소한다") {
                    fakePaymentClient.cancelledReservationIds shouldBe listOf(reservationRepository.findAll().single().id)
                }
                then("좌석은 예매 완료가 되지 않고 풀린다") {
                    fixture.seatStates(performanceId, listOf(1, 2)) shouldBe List(2) { SeatState("AVAILABLE", null, null) }
                }
                then("회차는 시간 초과로 끝난다") {
                    fixture.recordResults(userId) shouldBe listOf("TIMEOUT")
                }
            }
        }

        given("사용자가 좌석 1, 2를 선점했고, 공연 시작 시각이 이미 지났을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeats(userId, performanceId, listOf(1, 2))
            jdbcTemplate.update("UPDATE performances SET start_at = ? WHERE id = ?", LocalDateTime.now(testClock).minusSeconds(1), performanceId)

            `when`("결제를 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.reserve(userId, performanceId, "key-1") }

                then("결제를 부르지 않고 PERFORMANCE_ALREADY_STARTED로 거절한다") {
                    exception.errorCode shouldBe ReservationErrorCode.PERFORMANCE_ALREADY_STARTED
                    fakePaymentClient.approvedRequests.size shouldBe 0
                }
            }
        }
    }
}
