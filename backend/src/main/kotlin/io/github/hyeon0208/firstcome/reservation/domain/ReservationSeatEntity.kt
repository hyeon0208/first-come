package io.github.hyeon0208.firstcome.reservation.domain

import io.github.hyeon0208.firstcome.support.domain.BaseEntity
import io.github.hyeon0208.firstcome.support.error.requireRule
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "reservation_seats")
class ReservationSeatEntity(
    @Column(nullable = false)
    val reservationId: Long,

    @Column(nullable = false)
    val performanceId: Long,

    @Column(nullable = false)
    val seatId: Long,

    // 예매 당시 가격
    @Column(nullable = false)
    val price: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    // 확정이면 true, 아니면 null: (회차, 좌석, active) 유일 제약으로 한 좌석이 두 번 확정되는 것을 막는다.
    @Column
    var active: Boolean? = null
        protected set

    init {
        requireRule(price >= 0) { "좌석 가격은 0원 이상이어야 합니다." }
    }

    fun activate() {
        active = true
    }
}
