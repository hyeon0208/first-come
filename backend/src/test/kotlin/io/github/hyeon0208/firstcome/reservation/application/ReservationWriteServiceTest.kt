package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.reservation.application.dto.CreateReservationRequest
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationStart
import io.github.hyeon0208.firstcome.reservation.domain.ReservationErrorCode
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationRepository
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationSeatRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.github.hyeon0208.firstcome.support.test.SeatState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Duration
import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class ReservationWriteServiceTest : BaseServiceTest() {
    @Autowired
    private lateinit var reservationWriteService: ReservationWriteService

    @Autowired
    private lateinit var reservationRepository: JpaReservationRepository

    @Autowired
    private lateinit var reservationSeatRepository: JpaReservationSeatRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private fun request(userId: Long, performanceId: Long, performanceSeatIds: List<Long>) =
        CreateReservationRequest(userId = userId, performanceId = performanceId, performanceSeatIds = performanceSeatIds)

    init {
        given("사용자가 회차 1의 1행 좌석 19, 20을 임시 배정했을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(19, 20))
            val holdExpiresAt = LocalDateTime.now(testClock).plusMinutes(5)

            `when`("결제 대기 예약을 만들면") {
                val pending = fixture.createPendingReservation(userId, performanceId = 1, listOf(19, 20), "key-1")

                then("좌석 가격을 더한 금액을 돌려준다(150,000원 × 2석)") {
                    pending.amount shouldBe 300_000L
                }
                then("예약은 결제 대기 상태로 그 금액과 함께 저장된다") {
                    val reservation = reservationRepository.findById(pending.reservationId).orElseThrow()
                    reservation.status shouldBe ReservationStatus.PENDING
                    reservation.amount shouldBe 300_000L
                }
                then("예약 좌석은 좌석마다 등급 가격으로 저장된다") {
                    reservationSeatRepository.findAllByReservationId(pending.reservationId)
                        .associate { it.performanceSeatId to it.price } shouldBe mapOf(19L to 150_000L, 20L to 150_000L)
                }
                then("좌석은 결제 중으로 잠긴다(만료 시각은 그대로)") {
                    fixture.seatStates(listOf(19, 20)) shouldBe List(2) { SeatState("PAYING", userId, holdExpiresAt) }
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2를 임시 배정했을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))

            `when`("좌석 1만으로 결제 대기 예약을 만들면") {
                val exception = shouldThrow<BusinessException> { fixture.createPendingReservation(userId, performanceId = 1, listOf(1), "key-1") }

                then("부분 예약은 HOLD_MISMATCH로 거부된다") {
                    exception.errorCode shouldBe ReservationErrorCode.HOLD_MISMATCH
                }
                then("예약은 저장되지 않는다") {
                    reservationRepository.count() shouldBe 0L
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2를 임시 배정하고 5분 1초가 지났을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            testClock.advance(Duration.ofMinutes(5).plusSeconds(1))

            `when`("결제 대기 예약을 만들면") {
                val exception = shouldThrow<BusinessException> { fixture.createPendingReservation(userId, performanceId = 1, listOf(1, 2), "key-1") }

                then("만료된 임시 배정은 HOLD_EXPIRED로 거부된다") {
                    exception.errorCode shouldBe ReservationErrorCode.HOLD_EXPIRED
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2를 임시 배정하고 회차 시작 시각(2026-12-25 10:00)이 되었을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            testClock.advance(Duration.ofDays(24))

            `when`("결제 대기 예약을 만들면") {
                val exception = shouldThrow<BusinessException> { fixture.createPendingReservation(userId, performanceId = 1, listOf(1, 2), "key-1") }

                then("PERFORMANCE_ALREADY_STARTED로 거부된다") {
                    exception.errorCode shouldBe PerformanceErrorCode.PERFORMANCE_ALREADY_STARTED
                }
            }
        }

        given("사용자가 멱등 키 key-1로 회차 1의 좌석 1, 2의 결제 대기 예약을 만들었고, 다른 사용자가 좌석 3, 4를 임시 배정했을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            val otherUserId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            fixture.createPendingReservation(userId, performanceId = 1, listOf(1, 2), "key-1")
            fixture.holdSeats(otherUserId, performanceId = 1, listOf(3, 4))

            `when`("다른 사용자가 같은 멱등 키로 좌석 3, 4를 요청하면") {
                val start = reservationWriteService.startReservation(request(otherUserId, performanceId = 1, listOf(3, 4)), "key-1")

                then("멱등 키는 사용자마다 따로라 새 결제 대기 예약을 만든다") {
                    start.shouldBeInstanceOf<ReservationStart.Pending>()
                }
            }

            `when`("같은 멱등 키, 같은 내용으로 요청하면") {
                val exception = shouldThrow<BusinessException> {
                    reservationWriteService.startReservation(request(userId, performanceId = 1, listOf(1, 2)), "key-1")
                }

                then("처리 중이므로 IDEMPOTENCY_KEY_IN_PROGRESS다") {
                    exception.errorCode shouldBe ReservationErrorCode.IDEMPOTENCY_KEY_IN_PROGRESS
                }
            }

            `when`("같은 멱등 키에 좌석만 바꿔(1, 3) 요청하면") {
                val exception = shouldThrow<BusinessException> {
                    reservationWriteService.startReservation(request(userId, performanceId = 1, listOf(1, 3)), "key-1")
                }

                then("좌석이 다르므로 IDEMPOTENCY_KEY_MISMATCH다") {
                    exception.errorCode shouldBe ReservationErrorCode.IDEMPOTENCY_KEY_MISMATCH
                }
            }

            `when`("같은 멱등 키에 회차만 바꿔(2) 요청하면") {
                val exception = shouldThrow<BusinessException> {
                    reservationWriteService.startReservation(request(userId, performanceId = 2, listOf(1, 2)), "key-1")
                }

                then("회차가 다르므로 IDEMPOTENCY_KEY_MISMATCH다") {
                    exception.errorCode shouldBe ReservationErrorCode.IDEMPOTENCY_KEY_MISMATCH
                }
            }
        }

        given("사용자가 멱등 키 key-1로 회차 1의 좌석 1, 2의 예약을 확정했을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            val confirmed = fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1")

            `when`("같은 멱등 키로 좌석 순서만 바꿔 요청하면") {
                val start = reservationWriteService.startReservation(request(userId, performanceId = 1, listOf(2, 1)), "key-1")

                then("같은 요청으로 보고 확정된 예약을 돌려준다") {
                    start shouldBe ReservationStart.AlreadyDone(confirmed)
                }
            }
        }

        given("사용자가 멱등 키 key-1로 만든 결제 대기 예약이 실패로 끝났을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            val pending = fixture.createPendingReservation(userId, performanceId = 1, listOf(1, 2), "key-1")
            reservationWriteService.failIfPending(pending, null)

            `when`("같은 멱등 키로 요청하면") {
                val exception = shouldThrow<BusinessException> {
                    reservationWriteService.startReservation(request(userId, performanceId = 1, listOf(1, 2)), "key-1")
                }

                then("IDEMPOTENCY_KEY_ALREADY_FAILED다") {
                    exception.errorCode shouldBe ReservationErrorCode.IDEMPOTENCY_KEY_ALREADY_FAILED
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2로 결제 대기 예약을 만들었을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            val pending = fixture.createPendingReservation(userId, performanceId = 1, listOf(1, 2), "key-1")

            `when`("결제 승인(TX-1)으로 예약을 확정하면") {
                reservationWriteService.confirmReservation(pending, PaymentApproval(transactionId = "TX-1"))

                then("예약은 확정 상태가 되고 확정 시각이 남는다") {
                    val reservation = reservationRepository.findById(pending.reservationId).orElseThrow()
                    reservation.status shouldBe ReservationStatus.CONFIRMED
                    reservation.confirmedAt shouldBe LocalDateTime.now(testClock)
                }
                then("좌석은 예약 완료가 되고 임시 배정 정보는 지워진다") {
                    fixture.seatStates(listOf(1, 2)) shouldBe List(2) { SeatState("RESERVED", null, null) }
                }
                then("승인 번호가 예약에 저장된다") {
                    reservationRepository.findById(pending.reservationId).orElseThrow().paymentTransactionId shouldBe "TX-1"
                }
                then("예약 좌석이 확정 좌석으로 표시된다(active)") {
                    reservationSeatRepository.findAllByReservationId(pending.reservationId).map { it.active } shouldBe listOf(true, true)
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2로 결제 대기 예약을 만든 뒤, 임시 배정 만료 시각이 지났을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            val pending = fixture.createPendingReservation(userId, performanceId = 1, listOf(1, 2), "key-1")
            testClock.advance(Duration.ofMinutes(5).plusSeconds(1))

            `when`("예약을 확정하면") {
                val exception = shouldThrow<BusinessException> {
                    reservationWriteService.confirmReservation(pending, PaymentApproval(transactionId = "TX-1"))
                }

                then("BOOKING_TIME_EXPIRED로 거부된다") {
                    exception.errorCode shouldBe ReservationErrorCode.BOOKING_TIME_EXPIRED
                }
                then("좌석은 예약 완료로 바뀌지 않는다") {
                    fixture.seatStates(listOf(1, 2)).map { it.status } shouldBe listOf("PAYING", "PAYING")
                }
                then("예약은 결제 대기 그대로이고 승인 번호는 저장되지 않는다") {
                    val reservation = reservationRepository.findById(pending.reservationId).orElseThrow()
                    reservation.status shouldBe ReservationStatus.PENDING
                    reservation.paymentTransactionId shouldBe null
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2로 결제 대기 예약을 만들어 좌석이 결제 중이고, 임시 배정 만료 시각이 지났을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            val otherUserId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            val holdExpiresAt = LocalDateTime.now(testClock).plusMinutes(5)
            fixture.createPendingReservation(userId, performanceId = 1, listOf(1, 2), "key-1")
            testClock.advance(Duration.ofMinutes(5).plusSeconds(1))

            `when`("다른 사용자가 좌석 2를 임시 배정하려 하면") {
                val exception = shouldThrow<BusinessException> { fixture.holdSeats(otherUserId, performanceId = 1, listOf(2)) }

                then("결제 중인 좌석은 만료 시각이 지나도 풀리지 않아 SEAT_NOT_AVAILABLE로 거부된다") {
                    exception.errorCode shouldBe PerformanceErrorCode.SEAT_NOT_AVAILABLE
                }
                then("좌석은 그 사용자의 결제 중 그대로다") {
                    fixture.seatStates(listOf(1, 2)) shouldBe List(2) { SeatState("PAYING", userId, holdExpiresAt) }
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2로 만든 예약이 결제 대기일 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            val holdExpiresAt = LocalDateTime.now(testClock).plusMinutes(5)
            val pending = fixture.createPendingReservation(userId, performanceId = 1, listOf(1, 2), "key-1")

            `when`("결제 승인 번호(TX-1)와 함께 결제 대기일 때만 실패로 바꾸면") {
                val failed = reservationWriteService.failIfPending(pending, "TX-1")

                then("true를 돌려주고 예약은 실패 상태가 된다") {
                    failed shouldBe true
                    reservationRepository.findById(pending.reservationId).orElseThrow().status shouldBe ReservationStatus.FAILED
                }
                then("승인 번호가 예약에 남는다") {
                    reservationRepository.findById(pending.reservationId).orElseThrow().paymentTransactionId shouldBe "TX-1"
                }
                then("확정하지 못한 좌석은 풀려 예약 가능이 된다") {
                    fixture.seatStates(listOf(1, 2)) shouldBe List(2) { SeatState("AVAILABLE", null, null) }
                }
            }
        }

        given("사용자가 회차 1의 좌석 1, 2로 만든 예약이 이미 확정됐을 때(확정은 커밋됐는데 응답 전에 연결이 끊긴 경우)") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            val pending = fixture.createPendingReservation(userId, performanceId = 1, listOf(1, 2), "key-1")
            reservationWriteService.confirmReservation(pending, PaymentApproval(transactionId = "TX-1"))

            `when`("결제 대기일 때만 실패로 바꾸면") {
                val failed = reservationWriteService.failIfPending(pending, "TX-1")

                then("false를 돌려주고 예약은 확정 그대로다") {
                    failed shouldBe false
                    reservationRepository.findById(pending.reservationId).orElseThrow().status shouldBe ReservationStatus.CONFIRMED
                }
            }
        }
    }
}
