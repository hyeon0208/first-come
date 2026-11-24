package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.reservation.domain.ReservationErrorCode
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.github.hyeon0208.firstcome.support.test.runConcurrently
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

// 같은 임시 배정에는 결제가 한 번에 한 건만 진행된다. 멱등 키가 달라도 진행 중에 온 요청은 결제를 부르지 않고 거절한다.
class ReservationSinglePaymentConcurrencyTest : BaseServiceTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private fun reservationStatuses(): List<String> =
        jdbcTemplate.query("SELECT status FROM reservations ORDER BY id") { rs, _ -> rs.getString("status") }

    init {
        given("사용자가 회차 1의 좌석 1, 2를 임시 배정했고, 다른 요청 4건이 끝날 때까지 결제 승인이 끝나지 않을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))
            val otherRequestsDone = CountDownLatch(4)
            fakePaymentClient.beforeApprove = { otherRequestsDone.await(10, TimeUnit.SECONDS) }

            `when`("서로 다른 멱등 키 5개로 동시에 예약 확정을 요청하면") {
                val results = runConcurrently(
                    List(5) { index ->
                        {
                            try {
                                fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-$index")
                            } catch (exception: BusinessException) {
                                otherRequestsDone.countDown()
                                throw exception
                            }
                        }
                    },
                )

                then("결제 승인은 정확히 1번이다") {
                    fakePaymentClient.approvedRequests.size shouldBe 1
                }
                then("1건만 성공하고 나머지 4건은 결제 진행 중(PAYMENT_IN_PROGRESS)으로 거절된다") {
                    results.count { it.isSuccess } shouldBe 1
                    results.mapNotNull { (it.exceptionOrNull() as? BusinessException)?.errorCode } shouldBe
                        List(4) { ReservationErrorCode.PAYMENT_IN_PROGRESS }
                }
                then("예약은 확정된 1건뿐이고 취소한 결제는 없다") {
                    reservationStatuses() shouldBe listOf("CONFIRMED")
                    fakePaymentClient.cancelledReservationIds shouldBe emptyList()
                }
            }
        }
    }
}
