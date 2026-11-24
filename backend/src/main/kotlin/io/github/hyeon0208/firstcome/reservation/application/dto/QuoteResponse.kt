package io.github.hyeon0208.firstcome.reservation.application.dto

// 선점한 좌석으로 결제할 금액. 티켓 금액에 장당 예매수수료를 더한다.
data class QuoteResponse(
    val seats: List<ReservationSeatResponse>,
    val ticketAmount: Long,
    val feeAmount: Long,
    val amount: Long,
) {
    companion object {
        fun of(seats: List<ReservationSeatResponse>, feePerTicket: Long): QuoteResponse {
            val ticketAmount = seats.sumOf { it.price }
            val feeAmount = feePerTicket * seats.size
            return QuoteResponse(seats = seats, ticketAmount = ticketAmount, feeAmount = feeAmount, amount = ticketAmount + feeAmount)
        }
    }
}
