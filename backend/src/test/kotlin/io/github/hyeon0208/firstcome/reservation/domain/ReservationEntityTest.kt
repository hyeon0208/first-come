package io.github.hyeon0208.firstcome.reservation.domain

import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.error.CommonErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class ReservationEntityTest : BehaviorSpec({
    val now = LocalDateTime.of(2026, 12, 1, 10, 0)

    fun reservation(idempotencyKey: String) =
        ReservationEntity(userId = 1, performanceId = 1, idempotencyKey = idempotencyKey, amount = 150_000)

    given("예약 규칙(멱등 키 1~64자)으로") {
        `when`("64자 멱등 키로 예약을 만들면") {
            val created = reservation(idempotencyKey = "k".repeat(64))

            then("결제 대기 상태로 만들어진다") {
                created.status shouldBe ReservationStatus.PENDING
            }
        }

        `when`("빈 멱등 키로 예약을 만들면") {
            val exception = shouldThrow<BusinessException> { reservation(idempotencyKey = "") }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }

        `when`("65자 멱등 키로 예약을 만들면") {
            val exception = shouldThrow<BusinessException> { reservation(idempotencyKey = "k".repeat(65)) }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }
    }

    given("결제 대기 예약이 있을 때") {
        val pending = reservation(idempotencyKey = "key-1")

        `when`("결제 승인 번호로 확정하면") {
            pending.confirm(paymentTransactionId = "TX-1", now = now)

            then("확정 상태가 되고 승인 번호와 확정 시각이 남는다") {
                pending.status shouldBe ReservationStatus.CONFIRMED
                pending.paymentTransactionId shouldBe "TX-1"
                pending.confirmedAt shouldBe now
            }
        }
    }

    given("결제가 거절된 결제 대기 예약이 있을 때") {
        val pending = reservation(idempotencyKey = "key-1")

        `when`("승인 번호 없이 실패로 바꾸면") {
            pending.fail(paymentTransactionId = null)

            then("실패 상태가 되고 승인 번호와 확정 시각은 남지 않는다") {
                pending.status shouldBe ReservationStatus.FAILED
                pending.paymentTransactionId shouldBe null
                pending.confirmedAt shouldBe null
            }
        }
    }

    given("결제는 승인됐지만 결제 중 좌석을 잃은 결제 대기 예약이 있을 때") {
        val pending = reservation(idempotencyKey = "key-1")

        `when`("결제 승인 번호와 함께 실패로 바꾸면") {
            pending.fail(paymentTransactionId = "TX-1")

            then("실패 상태가 되고 대사에 쓸 승인 번호가 남는다") {
                pending.status shouldBe ReservationStatus.FAILED
                pending.paymentTransactionId shouldBe "TX-1"
                pending.confirmedAt shouldBe null
            }
        }
    }

    given("이미 확정된 예약이 있을 때") {
        val confirmed = reservation(idempotencyKey = "key-1").apply { confirm(paymentTransactionId = "TX-1", now = now) }

        `when`("확정된 예약을 다시 확정하면") {
            shouldThrow<IllegalStateException> { confirmed.confirm(paymentTransactionId = "TX-1", now = now.plusMinutes(1)) }

            then("처음 확정 시각이 그대로 남는다") {
                confirmed.confirmedAt shouldBe now
            }
        }

        `when`("확정된 예약을 실패로 바꾸면") {
            shouldThrow<IllegalStateException> { confirmed.fail(paymentTransactionId = null) }

            then("확정 상태 그대로다") {
                confirmed.status shouldBe ReservationStatus.CONFIRMED
            }
        }
    }

})
