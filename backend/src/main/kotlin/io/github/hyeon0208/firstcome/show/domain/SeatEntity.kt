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
@Table(name = "seats")
class SeatEntity(
    @Column(nullable = false)
    val venueId: Long,

    @Column(nullable = false)
    val floor: Int,

    @Column(nullable = false, length = 1)
    val block: String,

    @Column(nullable = false)
    val rowNo: Int,

    @Column(nullable = false)
    val seatNo: Int,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    val grade: SeatGrade,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    // 예: "1층 B구역 3열 1번"
    val displayName: String
        get() = "${floor}층 ${block}구역 ${rowNo}열 ${seatNo}번"
}
