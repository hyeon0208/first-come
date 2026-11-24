package io.github.hyeon0208.firstcome.reservation.domain

import io.github.hyeon0208.firstcome.support.domain.BaseEntity
import io.github.hyeon0208.firstcome.support.error.requireRule
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "reservation_seats",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_reservation_seats_reservation_performance_seat", columnNames = ["reservation_id", "performance_seat_id"]),
        UniqueConstraint(name = "uk_reservation_seats_active_performance_seat", columnNames = ["performance_seat_id", "active"]),
    ],
)
class ReservationSeatEntity(
    @Column(nullable = false)
    val reservationId: Long,

    @Column(nullable = false)
    val performanceId: Long,

    @Column(nullable = false)
    val performanceSeatId: Long,

    @Column(nullable = false)
    val price: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    // 확정이면 true, 아니면 null: (좌석, active) 유니크로 한 좌석이 두 번 확정되는 것만 막는다.
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
