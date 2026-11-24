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
import org.springframework.beans.factory.annotation.Autowired

// 결제를 부르기 전에 확인한 조건(임시 배정 만료, 공연 시작 시각)은 결제가 승인된 뒤 확정 단계에서 다시 확인한다.
class ReservationRecheckTest : BaseServiceTest() {
    @Autowired
    private lateinit var reservationRepository: JpaReservationRepository

    init {
        given("사용자가 회차 1의 좌석 1, 2를 임시 배정했고, 결제하는 동안 임시 배정 만료 시각이 지날 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            fakePaymentClient.beforeApprove = { testClock.advance(Duration.ofMinutes(5).plusSeconds(1)) }

            `when`("예약 확정을 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1") }

                then("시간 초과(BOOKING_TIME_EXPIRED)로 거절한다") {
                    exception.errorCode shouldBe ReservationErrorCode.BOOKING_TIME_EXPIRED
                }
                then("예약을 확정하지 않는다") {
                    reservationRepository.findAll().single().status shouldBe ReservationStatus.FAILED
                }
                then("승인된 결제를 자동으로 취소한다") {
                    val reservationId = reservationRepository.findAll().single().id
                    fakePaymentClient.cancelledReservationIds shouldBe listOf(reservationId)
                }
                then("좌석은 예약 완료가 되지 않고 풀려 예약 가능이 된다") {
                    fixture.seatStates(listOf(1, 2)) shouldBe List(2) { SeatState("AVAILABLE", null, null) }
                }
            }
        }

        given("사용자가 공연 시작 2분 전에 회차 1의 좌석 1, 2를 임시 배정했고, 결제하는 동안 공연 시작 시각(2026-12-25 10:00)이 지날 때") {
            loadInitialData()
            val userId = fixture.createUser()
            testClock.advance(Duration.ofDays(24).minusMinutes(2))
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            fakePaymentClient.beforeApprove = { testClock.advance(Duration.ofMinutes(3)) }

            `when`("예약 확정을 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1") }

                then("임시 배정은 아직 2분 남았어도 시간 초과(BOOKING_TIME_EXPIRED)로 거절한다") {
                    exception.errorCode shouldBe ReservationErrorCode.BOOKING_TIME_EXPIRED
                }
                then("예약을 확정하지 않는다") {
                    reservationRepository.findAll().single().status shouldBe ReservationStatus.FAILED
                }
                then("승인된 결제를 자동으로 취소한다") {
                    val reservationId = reservationRepository.findAll().single().id
                    fakePaymentClient.cancelledReservationIds shouldBe listOf(reservationId)
                }
                then("좌석은 예약 완료가 되지 않고 풀려 예약 가능이 된다") {
                    fixture.seatStates(listOf(1, 2)) shouldBe List(2) { SeatState("AVAILABLE", null, null) }
                }
            }
        }
    }
}
