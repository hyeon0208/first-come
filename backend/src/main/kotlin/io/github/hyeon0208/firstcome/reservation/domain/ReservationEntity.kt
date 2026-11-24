package io.github.hyeon0208.firstcome.reservation.domain

import io.github.hyeon0208.firstcome.support.domain.BaseEntity
import io.github.hyeon0208.firstcome.support.error.requireRule
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "reservations",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_reservations_user_idempotency_key", columnNames = ["user_id", "idempotency_key"]),
        UniqueConstraint(name = "uk_reservations_id_performance", columnNames = ["id", "performance_id"]),
    ],
)
class ReservationEntity(
    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val performanceId: Long,

    @Column(nullable = false, length = IDEMPOTENCY_KEY_MAX_LENGTH)
    val idempotencyKey: String,

    @Column(nullable = false)
    val amount: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    var status: ReservationStatus = ReservationStatus.PENDING
        protected set

    @Column(length = 64)
    var paymentTransactionId: String? = null
        protected set

    var confirmedAt: LocalDateTime? = null
        protected set

    init {
        requireRule(amount >= 0) { "결제 금액은 0원 이상이어야 합니다." }
        requireRule(idempotencyKey.length in 1..IDEMPOTENCY_KEY_MAX_LENGTH) { "멱등 키는 1~${IDEMPOTENCY_KEY_MAX_LENGTH}자여야 합니다." }
    }

    fun confirm(paymentTransactionId: String, now: LocalDateTime) {
        check(status == ReservationStatus.PENDING) { "결제 대기 상태의 예약만 확정할 수 있습니다. reservationId=$id status=$status" }
        status = ReservationStatus.CONFIRMED
        this.paymentTransactionId = paymentTransactionId
        confirmedAt = now
    }

    fun fail(paymentTransactionId: String?) {
        check(status == ReservationStatus.PENDING) { "결제 대기 상태의 예약만 실패로 바꿀 수 있습니다. reservationId=$id status=$status" }
        status = ReservationStatus.FAILED
        this.paymentTransactionId = paymentTransactionId
    }

    companion object {
        const val IDEMPOTENCY_KEY_MAX_LENGTH = 64
    }
}
