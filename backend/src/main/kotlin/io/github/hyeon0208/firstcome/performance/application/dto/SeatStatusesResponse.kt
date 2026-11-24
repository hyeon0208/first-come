package io.github.hyeon0208.firstcome.performance.application.dto

// 좌석 상태. 화면은 선택 가능, 선택 불가, 내 선택 세 가지만 구분한다(판매됨과 남이 선점함을 구분하지 않는다).
data class SeatStatusesResponse(
    val unavailableSeatIds: List<Long>,
    val mySeatIds: List<Long>,
    val soldOut: Boolean,
)
