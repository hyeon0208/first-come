package io.github.hyeon0208.firstcome.paymentmock

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component

// 주문 번호별 결제 상태.
// - 같은 주문 번호로 다시 승인을 요청하면 승인을 새로 만들지 않는다.
// - 취소된 주문은 그 뒤에 승인하지 않는다. 승인이 늦어지는 사이 부른 쪽이 먼저 취소를 보냈을 때, 늦은 승인이 남지 않게 한다.
// shortcut: 주문 상태를 지우지 않아 주문 수만큼 메모리가 는다. 오래 띄워 두는 부하 측정에서 문제가 되면 만료를 넣는다.
@Component
class PaymentStore {
    private val stateByOrderId = ConcurrentHashMap<Long, OrderState>()

    // 승인 번호. 이미 취소된 주문이면 null이다.
    fun approve(orderId: Long): String? =
        stateByOrderId.compute(orderId) { _, state -> state ?: OrderState.Approved("PM-${UUID.randomUUID()}") }
            .let { (it as? OrderState.Approved)?.transactionId }

    // 승인된 결제가 있었으면 true. 없어도 오류가 아니다(응답을 받지 못한 쪽이 되돌리려고 부를 수 있다).
    fun cancel(orderId: Long): Boolean {
        var hadApproval = false
        stateByOrderId.compute(orderId) { _, state ->
            hadApproval = state is OrderState.Approved
            OrderState.Cancelled
        }
        return hadApproval
    }

    private sealed interface OrderState {
        data class Approved(val transactionId: String) : OrderState

        data object Cancelled : OrderState
    }
}
