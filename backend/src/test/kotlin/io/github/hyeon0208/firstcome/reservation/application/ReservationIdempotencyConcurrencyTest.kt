package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.reservation.domain.ReservationErrorCode
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.github.hyeon0208.firstcome.support.test.runConcurrently
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class ReservationIdempotencyConcurrencyTest : BaseServiceTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private fun reservations(): List<Pair<Long, String>> =
        jdbcTemplate.query("SELECT id, status FROM reservations") { rs, _ -> rs.getLong("id") to rs.getString("status") }

    init {
        given("사용자가 회차 1의 좌석 1, 2를 임시 배정했을 때") {
            loadInitialData()
            val userId = fixture.createUser()
            fixture.holdSeats(userId, performanceId = 1, listOf(1, 2))

            `when`("같은 멱등 키로 5개 요청이 동시에 예약 확정을 요청하면") {
                val results = runConcurrently(List(5) { { fixture.createReservation(userId, performanceId = 1, listOf(1, 2), "key-1") } })

                then("예약은 확정된 1건이고 결제는 한 번이다") {
                    reservations().map { it.second } shouldBe listOf("CONFIRMED")
                    fakePaymentClient.approvedRequests.size shouldBe 1
                }
                then("성공한 응답은 모두 그 예약이다") {
                    results.mapNotNull { it.getOrNull()?.reservationId }.distinct() shouldBe reservations().map { it.first }
                }
                then("실패한 응답은 모두 처리 중(IDEMPOTENCY_KEY_IN_PROGRESS)이다") {
                    results.mapNotNull { it.exceptionOrNull() }.forEach {
                        it.shouldBeInstanceOf<BusinessException>().errorCode shouldBe ReservationErrorCode.IDEMPOTENCY_KEY_IN_PROGRESS
                    }
                }
            }
        }
    }
}
