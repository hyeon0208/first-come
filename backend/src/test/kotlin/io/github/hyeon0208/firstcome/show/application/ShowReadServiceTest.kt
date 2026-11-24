package io.github.hyeon0208.firstcome.show.application

import io.github.hyeon0208.firstcome.show.SeatGrade
import io.github.hyeon0208.firstcome.support.test.BaseServiceTest
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired

class ShowReadServiceTest : BaseServiceTest() {
    @Autowired
    private lateinit var showReadService: ShowReadService

    init {
        given("고정 데이터로") {
            `when`("공연 목록을 보면") {
                val shows = showReadService.getShows()

                then("가상 공연 3개가 등급별 가격과 함께 나온다") {
                    shows.map { it.title } shouldBe listOf("모래시계 밴드 겨울 단독 공연 〈첫눈〉", "유리온실 3집 발매 기념 콘서트", "새벽 네 시 라디오 공개방송")
                    shows.map { it.venueName }.distinct() shouldBe listOf("연습장 홀")
                    shows[0].seatPrices.map { it.name to it.price } shouldBe listOf("R석" to 132_000L, "S석" to 99_000L)
                    shows[2].seatPrices.map { it.name to it.price } shouldBe listOf("전석" to 55_000L, "전석" to 55_000L)
                }
            }

            `when`("공연 1의 좌석 배치를 보면") {
                val seats = showReadService.getSeatMap(1).seats

                then("1,008석이고 R석은 1층 A·B구역 384석, S석은 나머지 624석이다") {
                    seats.size shouldBe 1_008
                    seats.count { it.grade == SeatGrade.R } shouldBe 384
                    seats.filter { it.grade == SeatGrade.R }.map { it.floor to it.block }.distinct() shouldBe listOf(1 to "A", 1 to "B")
                    seats.count { it.floor == 2 } shouldBe 240
                }
                then("좌석 이름은 층, 구역, 열, 번호 순서이고 좌석 번호는 그 순서대로 매겨진다") {
                    seats.first().let { it.seatId to it.name } shouldBe (1L to "1층 A구역 1열 1번")
                    seats.single { it.seatId == 193L }.name shouldBe "1층 B구역 1열 1번"
                    seats.last().name shouldBe "2층 C구역 4열 20번"
                }
            }
        }
    }
}
