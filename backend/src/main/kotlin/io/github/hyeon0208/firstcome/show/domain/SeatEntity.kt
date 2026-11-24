package io.github.hyeon0208.firstcome.show.domain

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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "seats",
    uniqueConstraints = [UniqueConstraint(name = "uk_seats_venue_position", columnNames = ["venue_id", "row_no", "column_no"])],
)
class SeatEntity(
    @Column(nullable = false)
    val venueId: Long,

    @Column(name = "row_no", nullable = false)
    val row: Int,

    @Column(name = "column_no", nullable = false)
    val column: Int,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    val grade: SeatGrade,

) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    init {
        requireRule(row >= 1 && column >= 1) { "좌석의 행과 열은 1 이상이어야 합니다." }
    }
}
