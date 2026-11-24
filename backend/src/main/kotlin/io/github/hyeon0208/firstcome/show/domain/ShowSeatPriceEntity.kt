package io.github.hyeon0208.firstcome.show.domain

import io.github.hyeon0208.firstcome.show.SeatGrade
import io.github.hyeon0208.firstcome.support.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "show_seat_prices")
class ShowSeatPriceEntity(
    @Column(nullable = false)
    val showId: Long,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    val grade: SeatGrade,

    // 화면에 보이는 등급 이름. 전석 공연은 두 등급의 이름이 같다.
    @Column(nullable = false, length = 10)
    val name: String,

    @Column(nullable = false)
    val price: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}
