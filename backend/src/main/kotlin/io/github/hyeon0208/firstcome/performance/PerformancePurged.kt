package io.github.hyeon0208.firstcome.performance

// 끝난 회차의 좌석 데이터를 지웠다. 그 회차에 딸린 다른 모듈의 데이터도 함께 지운다.
data class PerformancePurged(
    val performanceId: Long,
)
