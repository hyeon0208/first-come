package io.github.hyeon0208.firstcome.paymentmock

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component

// 주문 번호별 승인 내역. 같은 주문 번호로 다시 승인을 요청하면 승인을 새로 만들지 않는다.
@Component
class PaymentStore {
    private val transactionIdByOrderId = ConcurrentHashMap<Long, String>()

    fun approve(orderId: Long): String =
        transactionIdByOrderId.computeIfAbsent(orderId) { "PM-${UUID.randomUUID()}" }

    // 승인된 결제가 있었으면 true. 없어도 오류가 아니다(응답을 받지 못한 쪽이 되돌리려고 부를 수 있다).
    fun cancel(orderId: Long): Boolean =
        transactionIdByOrderId.remove(orderId) != null

    fun clear() {
        transactionIdByOrderId.clear()
    }
}
