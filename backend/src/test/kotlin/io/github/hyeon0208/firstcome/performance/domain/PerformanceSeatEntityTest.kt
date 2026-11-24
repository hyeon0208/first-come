package io.github.hyeon0208.firstcome.performance.domain

import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.error.CommonErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PerformanceSeatEntityTest : BehaviorSpec({
    val expiresAt = LocalDateTime.of(2026, 12, 1, 10, 5)

    given("만료 시각이 10:05인 임시 배정 좌석이 있을 때") {
        val seat = PerformanceSeatEntity(
            performanceId = 1,
            seatId = 1,
            status = PerformanceSeatStatus.HELD,
            holderId = 1,
            holdExpiresAt = expiresAt,
        )

        `when`("만료 시각과 그 1초 뒤에 상태를 보면") {
            val atExpiry = seat.statusAt(expiresAt)
            val afterExpiry = seat.statusAt(expiresAt.plusSeconds(1))

            then("만료 시각까지는 임시 배정이고, 지나면 예약 가능이다") {
                atExpiry shouldBe PerformanceSeatStatus.HELD
                afterExpiry shouldBe PerformanceSeatStatus.AVAILABLE
            }
        }
    }

    given("예약 완료된 좌석이 있을 때") {
        val seat = PerformanceSeatEntity(
            performanceId = 1,
            seatId = 1,
            status = PerformanceSeatStatus.RESERVED,
            holderId = null,
            holdExpiresAt = null,
        )

        `when`("임시 배정 만료와 상관없는 시각에 상태를 보면") {
            val status = seat.statusAt(expiresAt.plusDays(1))

            then("예약 완료 그대로다") {
                status shouldBe PerformanceSeatStatus.RESERVED
            }
        }
    }

    given("회차별 좌석 규칙(HELD면 임시 배정 사용자와 만료 시각이 있고, 그 밖의 상태면 둘 다 없다)으로") {
        `when`("임시 배정 사용자와 만료 시각이 있는 HELD 상태로 만들면") {
            val seat = PerformanceSeatEntity(
                performanceId = 1,
                seatId = 1,
                status = PerformanceSeatStatus.HELD,
                holderId = 1,
                holdExpiresAt = expiresAt,
            )

            then("만들어진다") {
                seat.status shouldBe PerformanceSeatStatus.HELD
            }
        }

        `when`("임시 배정 사용자와 만료 시각이 없는 RESERVED 상태로 만들면") {
            val seat = PerformanceSeatEntity(
                performanceId = 1,
                seatId = 1,
                status = PerformanceSeatStatus.RESERVED,
                holderId = null,
                holdExpiresAt = null,
            )

            then("만들어진다") {
                seat.status shouldBe PerformanceSeatStatus.RESERVED
            }
        }

        `when`("임시 배정 사용자가 없는 HELD 상태로 만들면") {
            val exception = shouldThrow<BusinessException> {
                PerformanceSeatEntity(
                    performanceId = 1,
                    seatId = 1,
                    status = PerformanceSeatStatus.HELD,
                    holderId = null,
                    holdExpiresAt = expiresAt,
                )
            }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }

        `when`("만료 시각이 있는 AVAILABLE 상태로 만들면") {
            val exception = shouldThrow<BusinessException> {
                PerformanceSeatEntity(
                    performanceId = 1,
                    seatId = 1,
                    status = PerformanceSeatStatus.AVAILABLE,
                    holderId = null,
                    holdExpiresAt = expiresAt,
                )
            }

            then("INVALID_REQUEST로 거부된다") {
                exception.errorCode shouldBe CommonErrorCode.INVALID_REQUEST
            }
        }
    }
})
