package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.github.hyeon0208.firstcome.user.domain.UserErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired

class ReservationReadServiceTest : BaseServiceTest() {
    @Autowired
    private lateinit var reservationReadService: ReservationReadService

    @Autowired
    private lateinit var reservationFacade: ReservationFacade

    private fun holdAndReserve(userId: Long, performanceId: Long, performanceSeatIds: List<Long>, idempotencyKey: String): ReservationResponse {
        fixture.holdSeats(userId, performanceId, performanceSeatIds)
        return fixture.createReservation(userId, performanceId, performanceSeatIds, idempotencyKey)
    }

    init {
        given("사용자에게 확정 2건(회차 1, 회차 2), 실패 1건이 있고 다른 사용자의 예약도 있을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            val otherUserId = fixture.createUser()
            val first = holdAndReserve(userId, performanceId = 1, listOf(1, 2), "key-1")
            val second = holdAndReserve(userId, performanceId = 2, listOf(401, 402), "key-2")
            fakePaymentClient.declinesPayment = true
            shouldThrow<BusinessException> { holdAndReserve(userId, performanceId = 1, listOf(5, 6), "key-4") }
            fakePaymentClient.declinesPayment = false
            holdAndReserve(otherUserId, performanceId = 1, listOf(7, 8), "key-1")

            `when`("내 예약을 조회하면") {
                val reservations = reservationReadService.getUserReservations(userId)

                then("확정된 내 예약만 최신순으로 돌려준다") {
                    reservations.map { it.reservationId to it.status } shouldBe listOf(
                        second.reservationId to ReservationStatus.CONFIRMED,
                        first.reservationId to ReservationStatus.CONFIRMED,
                    )
                }
                then("예약마다 자기 회차, 좌석, 금액이 붙는다") {
                    reservations.map { it.startAt } shouldBe listOf(
                        LocalDateTime.of(2026, 12, 25, 14, 0),
                        LocalDateTime.of(2026, 12, 25, 10, 0),
                    )
                    reservations.map { reservation -> reservation.seats.map { it.performanceSeatId } } shouldBe
                        listOf(listOf(401L, 402L), listOf(1L, 2L))
                    reservations.map { it.amount } shouldBe listOf(300_000L, 300_000L)
                }
            }
        }

        given("예약이 없는 사용자가 있을 때") {
            val userId = fixture.createUser()

            `when`("내 예약을 조회하면") {
                val reservations = reservationReadService.getUserReservations(userId)

                then("빈 목록이다") {
                    reservations.shouldBeEmpty()
                }
            }

            `when`("없는 사용자 id로 조회하면") {
                val exception = shouldThrow<BusinessException> { reservationReadService.getUserReservations(userId + 1) }

                then("USER_NOT_FOUND다") {
                    exception.errorCode shouldBe UserErrorCode.USER_NOT_FOUND
                }
            }
        }
    }
}
