package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationRepository
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class ReservationCompensationConcurrencyTest : BaseServiceTest() {
    @Autowired
    private lateinit var reservationWriteService: ReservationWriteService

    @Autowired
    private lateinit var reservationRepository: JpaReservationRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    // 확정 트랜잭션이 예약 행을 CONFIRMED로 바꾼 뒤, 커밋하기 전에 잠시 멈춘 상황을 만든다.
    private fun confirmWithDelayedCommit(reservationId: Long, updated: CountDownLatch): Thread =
        thread {
            TransactionTemplate(transactionManager).executeWithoutResult {
                jdbcTemplate.update("UPDATE reservations SET status = 'CONFIRMED' WHERE id = ?", reservationId)
                updated.countDown()
                Thread.sleep(500)
            }
        }

    init {
        given("사용자가 회차 1의 좌석 1, 2로 만든 예약이 결제 대기일 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            val pending = fixture.createPendingReservation(userId, performanceId = 1, listOf(1, 2), "key-1")

            `when`("확정 트랜잭션이 예약 행을 바꾸고 커밋하기 전에, 결제 대기일 때만 실패로 바꾸면") {
                val updated = CountDownLatch(1)
                val confirming = confirmWithDelayedCommit(pending.reservationId, updated)
                updated.await(5, TimeUnit.SECONDS) shouldBe true
                val failed = reservationWriteService.failIfPending(pending, "TX-1")
                confirming.join()

                then("확정 커밋을 기다렸다가 확정을 보고 false를 돌려준다") {
                    failed shouldBe false
                }
                then("예약은 확정 그대로다") {
                    reservationRepository.findById(pending.reservationId).orElseThrow().status shouldBe ReservationStatus.CONFIRMED
                }
            }
        }
    }
}
