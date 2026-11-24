package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.reservation.domain.ReservationErrorCode
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.time.Duration
import org.springframework.beans.factory.annotation.Autowired

class ReservationFacadeTest : BaseServiceTest() {
    @Autowired
    private lateinit var reservationFacade: ReservationFacade

    @Autowired
    private lateinit var reservationRepository: JpaReservationRepository

    init {
        given("사용자가 회차 1의 좌석 1, 2를 임시 배정했을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))

            `when`("예약 확정을 요청하면") {
                val response = fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1")

                then("결제 대기 예약의 금액으로 한 번 결제한다") {
                    fakePaymentClient.approvedRequests shouldBe listOf(
                        PaymentRequest(reservationId = response.reservationId, amount = 300_000L),
                    )
                }
                then("결제 승인 번호로 예약을 확정한다") {
                    response.status shouldBe ReservationStatus.CONFIRMED
                    reservationRepository.findById(response.reservationId).orElseThrow().paymentTransactionId shouldBe "FAKE-${response.reservationId}"
                }
            }
        }

        given("임시 배정이 없는 사용자가 있을 때") {
            loadInitialData()
            val userId = fixture.createUser()

            `when`("예약 확정을 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1") }

                then("결제 대기 단계의 오류(HOLD_NOT_FOUND)를 그대로 던진다") {
                    exception.errorCode shouldBe ReservationErrorCode.HOLD_NOT_FOUND
                }
                then("결제를 요청하지 않는다") {
                    fakePaymentClient.approvedRequests.shouldBeEmpty()
                }
            }
        }

        given("사용자가 멱등 키 key-1로 회차 1의 좌석 1, 2의 예약을 확정했을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            val first = fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1")

            `when`("같은 멱등 키로 다시 요청하면") {
                val response = fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1")

                then("첫 예약을 그대로 돌려준다") {
                    response shouldBe first
                }
                then("다시 결제하지 않는다") {
                    fakePaymentClient.approvedRequests.size shouldBe 1
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2를 임시 배정하고, 결제하는 동안 같은 멱등 키로 다시 요청하게 했을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            var innerException: BusinessException? = null
            fakePaymentClient.beforeApprove = {
                innerException = shouldThrow<BusinessException> { fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1") }
            }

            `when`("멱등 키 key-1로 예약 확정을 요청하면") {
                val response = fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1")

                then("결제하는 동안 들어온 요청은 IDEMPOTENCY_KEY_IN_PROGRESS다") {
                    innerException?.errorCode shouldBe ReservationErrorCode.IDEMPOTENCY_KEY_IN_PROGRESS
                }
                then("처음 요청은 확정되고 결제는 한 번이다") {
                    response.status shouldBe ReservationStatus.CONFIRMED
                    fakePaymentClient.approvedRequests.size shouldBe 1
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2를 임시 배정했고, 결제가 거절될 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            fakePaymentClient.declinesPayment = true

            `when`("예약 확정을 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1") }

                then("PAYMENT_DECLINED를 던진다") {
                    exception.errorCode shouldBe ReservationErrorCode.PAYMENT_DECLINED
                }
                then("예약을 실패로 바꾸고 승인 번호는 남기지 않는다") {
                    val reservation = reservationRepository.findAll().single()
                    reservation.status shouldBe ReservationStatus.FAILED
                    reservation.paymentTransactionId shouldBe null
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2를 임시 배정했고, 결제하는 동안 임시 배정이 만료되어 다른 사용자가 좌석 2를 임시 배정할 때") {
            loadInitialData()
            val userId = fixture.createUser()
            val otherUserId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            fakePaymentClient.beforeApprove = {
                testClock.advance(Duration.ofMinutes(5).plusSeconds(1))
                fixture.holdSeats(otherUserId, performanceId = 1, listOf(2))
            }

            `when`("예약 확정을 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1") }

                then("HOLD_LOST를 그대로 던진다") {
                    exception.errorCode shouldBe ReservationErrorCode.HOLD_LOST
                }
                then("승인한 결제를 취소한다") {
                    val reservationId = reservationRepository.findAll().single().id
                    fakePaymentClient.cancelledTransactionIds shouldBe listOf("FAKE-$reservationId")
                }
                then("예약을 실패로 바꾸고 취소한 결제의 승인 번호를 남긴다") {
                    val reservation = reservationRepository.findAll().single()
                    reservation.status shouldBe ReservationStatus.FAILED
                    reservation.paymentTransactionId shouldBe "FAKE-${reservation.id}"
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2를 임시 배정했고, 결제하는 동안 좌석 2를 잃은 데다 결제 취소까지 실패할 때") {
            loadInitialData()
            val userId = fixture.createUser()
            val otherUserId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            fakePaymentClient.beforeApprove = {
                testClock.advance(Duration.ofMinutes(5).plusSeconds(1))
                fixture.holdSeats(otherUserId, performanceId = 1, listOf(2))
            }
            fakePaymentClient.failsToCancel = true

            `when`("예약 확정을 요청하면") {
                val exception = shouldThrow<BusinessException> { fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1") }

                then("결제 취소 오류가 아니라 원래 오류(HOLD_LOST)를 던진다") {
                    exception.errorCode shouldBe ReservationErrorCode.HOLD_LOST
                }
                then("예약은 대사할 승인 번호와 함께 실패로 바뀌고, 결제는 취소되지 않은 채 남는다") {
                    val reservation = reservationRepository.findAll().single()
                    reservation.status shouldBe ReservationStatus.FAILED
                    reservation.paymentTransactionId shouldBe "FAKE-${reservation.id}"
                    fakePaymentClient.cancelledTransactionIds.shouldBeEmpty()
                }
            }
        }
    }
}
