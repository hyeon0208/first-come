package io.github.hyeon0208.firstcome.reservation.application.dto

// 결제를 부르기 전에 저장한 결제 대기 예매. 결제와 확정 단계가 이 값을 이어받는다.
data class PendingReservation(
    val reservationId: Long,
    val performanceId: Long,
    val participantNo: Int,
    val seatCount: Int,
    val amount: Long,
)
