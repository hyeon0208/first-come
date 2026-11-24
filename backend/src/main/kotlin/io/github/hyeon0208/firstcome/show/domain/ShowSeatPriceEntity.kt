package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.support.domain.BaseEntity
import io.github.hyeon0208.firstcome.support.error.requireRule
import io.github.hyeon0208.firstcome.show.domain.SeatGrade
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "show_seat_prices",
    uniqueConstraints = [UniqueConstraint(name = "uk_show_seat_prices_show_grade", columnNames = ["show_id", "grade"])],
)
class ShowSeatPriceEntity(
    @Column(nullable = false)
    val showId: Long,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    val grade: SeatGrade,

    @Column(nullable = false)
    val price: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    init {
        requireRule(price >= 0) { "좌석 가격은 0원 이상이어야 합니다." }
    }
}
