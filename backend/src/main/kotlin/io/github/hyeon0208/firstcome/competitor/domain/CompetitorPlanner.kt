package io.github.hyeon0208.firstcome.competitor.domain

import java.time.Duration
import kotlin.random.Random

// 회차의 난수 씨앗으로 가상 경쟁자들의 계획을 만든다. 같은 씨앗이면 같은 계획이 나온다.
object CompetitorPlanner {
    // 들어오는 시각은 arrivalWindow 안에서 고르게, 매수는 1~maxSeats에서 고르게(4매 한도면 평균 2.5매) 정한다.
    fun plan(seed: Long, count: Int, arrivalWindow: Duration, maxSeats: Int): List<CompetitorPlan> {
        val random = Random(seed)
        return (1..count)
            .map { participantNo ->
                CompetitorPlan(
                    participantNo = participantNo,
                    arrivalOffset = Duration.ofMillis(random.nextLong(arrivalWindow.toMillis() + 1)),
                    wantedSeats = random.nextInt(1, maxSeats + 1),
                    pickSeed = random.nextLong(),
                )
            }
            .sortedBy { it.arrivalOffset }
    }
}
