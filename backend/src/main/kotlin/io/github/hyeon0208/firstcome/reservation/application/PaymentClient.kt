package io.github.hyeon0208.firstcome.reservation.application

interface PaymentClient {
    fun pay(request: PaymentRequest): PaymentApproval

    // 예매 번호로 결제를 취소한다. 승인된 결제가 없어도 성공으로 끝나야 한다(응답을 받지 못한 결제를 되돌릴 때도 쓴다).
    fun cancel(reservationId: Long)
}
