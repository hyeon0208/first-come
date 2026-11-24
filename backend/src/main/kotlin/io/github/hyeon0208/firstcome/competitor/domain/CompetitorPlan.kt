package io.github.hyeon0208.firstcome.competitor.domain

import java.time.Duration

// 가상 경쟁자 한 명의 계획: 예매 오픈 뒤 언제 들어와서 몇 석을 노리는가.
// pickSeed는 그 경쟁자가 좌석을 고를 때 쓰는 난수 씨앗이다.
data class CompetitorPlan(
    val participantNo: Int,
    val arrivalOffset: Duration,
    val wantedSeats: Int,
    val pickSeed: Long,
)
