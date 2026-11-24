package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.performance.application.BookingFacade
import io.github.hyeon0208.firstcome.performance.application.PerformanceWriteService
import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
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

class ReservationFacadeTest : BaseServiceTest() {
    @Autowired
    private lateinit var reservationReadService: ReservationReadService

    @Autowired
    private lateinit var reservationRepository: JpaReservationRepository

    @Autowired
    private lateinit var performanceWriteService: PerformanceWriteService

    @Autowired
    private lateinit var bookingFacade: BookingFacade

    private fun errorCodeOf(block: () -> Unit) = shouldThrow<BusinessException>(block).errorCode

    init {
        given("사용자가 R석 좌석 1, 2를 선점했을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeats(userId, performanceId, listOf(1, 2))

            `when`("결제할 금액을 보면") {
                val quote = reservationReadService.getQuote(userId, performanceId)

                then("티켓 금액에 장당 예매수수료 2,000원을 더한다") {
                    quote.ticketAmount shouldBe 264_000L
                    quote.feeAmount shouldBe 4_000L
                    quote.amount shouldBe 268_000L
                    quote.seats.map { it.name } shouldBe listOf("1층 A구역 1열 1번", "1층 A구역 1열 2번")
                }
            }

            `when`("결제를 요청하면") {
                val response = fixture.reserve(userId, performanceId, "key-1")

                then("그 금액으로 한 번 결제하고 예매를 확정한다") {
                    fakePaymentClient.approvedRequests shouldBe listOf(PaymentRequest(reservationId = response.reservationId, amount = 268_000L))
                    response.status shouldBe ReservationStatus.CONFIRMED
                    reservationRepository.findById(response.reservationId).orElseThrow().paymentTransactionId shouldBe "FAKE-${response.reservationId}"
                }
                then("좌석은 예매 완료가 된다") {
                    fixture.seatStates(performanceId, listOf(1, 2)) shouldBe List(2) { SeatState("RESERVED", 0, null) }
                }
                then("회차는 성공으로 끝나고, 예매 완료 화면에서 예매를 볼 수 있다") {
                    fixture.recordResults(userId) shouldBe listOf("SUCCESS")
                    reservationReadService.getConfirmedReservation(userId, performanceId).reservationNo shouldBe response.reservationNo
                }
                then("회차 데이터가 지워지면 예매도 함께 지워진다") {
                    performanceWriteService.purge(performanceId)
                    reservationRepository.count() shouldBe 0L
                    errorCodeOf { reservationReadService.getConfirmedReservation(userId, performanceId) } shouldBe ReservationErrorCode.RESERVATION_NOT_FOUND
                }
            }
        }

        given("사용자가 좌석을 가선점만 하고 선택 완료를 누르지 않았을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            bookingFacade.preselect(userId, performanceId, 1)

            `when`("결제를 요청하면") {
                then("결제를 부르지 않고 HOLD_NOT_FOUND로 거절한다") {
                    errorCodeOf { fixture.reserve(userId, performanceId, "key-1") } shouldBe ReservationErrorCode.HOLD_NOT_FOUND
                    fakePaymentClient.approvedRequests.size shouldBe 0
                }
            }
        }

        given("사용자가 좌석 1, 2를 선점했고, 결제하는 동안 같은 멱등 키로 다시 요청할 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeats(userId, performanceId, listOf(1, 2))
            var retryErrorCode: Any? = null
            fakePaymentClient.beforeApprove = {
                fakePaymentClient.beforeApprove = null
                retryErrorCode = errorCodeOf { fixture.reserve(userId, performanceId, "key-1") }
            }

            `when`("결제를 요청하면") {
                val response = fixture.reserve(userId, performanceId, "key-1")

                then("다시 온 요청은 처리 중(IDEMPOTENCY_KEY_IN_PROGRESS)으로 거절되고, 처음 요청은 확정되며 결제는 한 번이다") {
                    retryErrorCode shouldBe ReservationErrorCode.IDEMPOTENCY_KEY_IN_PROGRESS
                    response.status shouldBe ReservationStatus.CONFIRMED
                    fakePaymentClient.approvedRequests.size shouldBe 1
                }
            }
        }

        given("사용자가 좌석 1, 2를 선점했고, 결제가 거절될 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeats(userId, performanceId, listOf(1, 2))
            fakePaymentClient.declinesPayment = true

            `when`("결제를 요청하면") {
                val errorCode = errorCodeOf { fixture.reserve(userId, performanceId, "key-1") }

                then("PAYMENT_DECLINED로 거절하고 예매는 승인 번호 없이 실패로 남는다") {
                    errorCode shouldBe ReservationErrorCode.PAYMENT_DECLINED
                    val reservation = reservationRepository.findAll().single()
                    reservation.status shouldBe ReservationStatus.FAILED
                    reservation.paymentTransactionId shouldBe null
                }
                then("같은 멱등 키로는 다시 결제할 수 없다") {
                    errorCodeOf { fixture.reserve(userId, performanceId, "key-1") } shouldBe ReservationErrorCode.IDEMPOTENCY_KEY_ALREADY_FAILED
                }
                then("좌석은 선점으로 돌아와 새 멱등 키로 다시 결제할 수 있다") {
                    fixture.seatStates(performanceId, listOf(1, 2)).map { it.status } shouldBe listOf("HELD", "HELD")
                    fakePaymentClient.declinesPayment = false
                    fixture.reserve(userId, performanceId, "key-2").status shouldBe ReservationStatus.CONFIRMED
                }
            }
        }

        given("사용자가 좌석 1, 2를 선점했고, 결제는 승인됐는데 응답이 돌아오지 않을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeats(userId, performanceId, listOf(1, 2))
            fakePaymentClient.losesResponse = true

            `when`("결제를 요청하면") {
                val errorCode = errorCodeOf { fixture.reserve(userId, performanceId, "key-1") }

                then("PAYMENT_UNAVAILABLE로 답하고, 승인됐을 수 있는 결제를 예매 번호로 취소한다") {
                    errorCode shouldBe ReservationErrorCode.PAYMENT_UNAVAILABLE
                    fakePaymentClient.cancelledReservationIds shouldBe listOf(reservationRepository.findAll().single().id)
                }
                then("예매는 실패로 바뀌고 좌석은 선점으로 돌아온다") {
                    reservationRepository.findAll().single().status shouldBe ReservationStatus.FAILED
                    fixture.seatStates(performanceId, listOf(1, 2)).map { it.status } shouldBe listOf("HELD", "HELD")
                }
            }
        }

        given("사용자가 좌석 1, 2를 선점했고, 결제 응답도 돌아오지 않고 결제 취소도 실패했을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeats(userId, performanceId, listOf(1, 2))
            fakePaymentClient.losesResponse = true
            fakePaymentClient.failsToCancel = true
            shouldThrow<BusinessException> { fixture.reserve(userId, performanceId, "key-1") }

            `when`("새 멱등 키로 다시 결제를 요청하면") {
                val errorCode = errorCodeOf { fixture.reserve(userId, performanceId, "key-2") }

                then("취소되지 않은 결제가 있을 수 있으므로 결제를 다시 부르지 않고 PAYMENT_IN_PROGRESS로 거절한다") {
                    errorCode shouldBe ReservationErrorCode.PAYMENT_IN_PROGRESS
                    fakePaymentClient.approvedRequests.size shouldBe 1
                }
                then("좌석은 결제 중으로 묶여 있다") {
                    fixture.seatStates(performanceId, listOf(1, 2)).map { it.status } shouldBe listOf("PAYING", "PAYING")
                }
            }

            `when`("결제 중에 처음부터 다시를 누르면") {
                then("SEATS_PAYING으로 거절된다") {
                    errorCodeOf { performanceWriteService.abandon(userId) } shouldBe PerformanceErrorCode.SEATS_PAYING
                }
            }

            `when`("예매 가능 시간이 끝나고 결제 결과를 기다리는 1분도 지나면") {
                testClock.advance(Duration.ofMinutes(8).plusSeconds(1))

                then("묶인 좌석이 회차를 붙잡지 않도록 시간 초과로 끝낸다") {
                    errorCodeOf { bookingFacade.getState(userId, performanceId) } shouldBe PerformanceErrorCode.TIME_OVER
                    fixture.recordResults(userId) shouldBe listOf("TIMEOUT")
                }
            }
        }

        given("사용자가 좌석 1, 2를 선점했고, 결제하는 동안 예매 가능 시간이 끝난 데다 결제 취소까지 실패할 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeats(userId, performanceId, listOf(1, 2))
            fakePaymentClient.beforeApprove = { testClock.advance(Duration.ofMinutes(7).plusSeconds(1)) }
            fakePaymentClient.failsToCancel = true

            `when`("결제를 요청하면") {
                val errorCode = errorCodeOf { fixture.reserve(userId, performanceId, "key-1") }

                then("결제 취소 오류가 아니라 원래 오류(BOOKING_TIME_EXPIRED)를 던진다") {
                    errorCode shouldBe ReservationErrorCode.BOOKING_TIME_EXPIRED
                }
                then("예매는 대사할 승인 번호와 함께 실패로 남는다") {
                    val reservation = reservationRepository.findAll().single()
                    reservation.status shouldBe ReservationStatus.FAILED
                    reservation.paymentTransactionId shouldBe "FAKE-${reservation.id}"
                }
            }
        }

        given("다른 참가자가 좌석 1, 2를 선점했을 때") {
            val userId = fixture.createUser()
            val performanceId = fixture.enterPerformance(userId)
            fixture.holdSeatsAs(performanceId, participantNo = 1, listOf(1, 2))

            `when`("그 참가자가 결제하면") {
                val response = reservationFacade.reserve(performanceId, 1, "competitor-1")

                then("사용자와 같은 경로로 예매가 확정되고 좌석은 그 참가자의 예매 완료가 된다") {
                    response.status shouldBe ReservationStatus.CONFIRMED
                    fixture.seatStates(performanceId, listOf(1, 2)) shouldBe List(2) { SeatState("RESERVED", 1, null) }
                }
                then("사용자의 회차는 끝나지 않는다") {
                    fixture.performanceResult(performanceId) shouldBe null
                }
            }
        }
    }

    @Autowired
    private lateinit var reservationFacade: ReservationFacade
}
