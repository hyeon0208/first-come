package io.github.hyeon0208.firstcome.performance.domain

import io.github.hyeon0208.firstcome.support.domain.BaseEntity
import io.github.hyeon0208.firstcome.support.error.requireRule
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Duration
import java.time.LocalDateTime
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "performance_seats",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_performance_seats_performance_seat", columnNames = ["performance_id", "seat_id"]),
        UniqueConstraint(name = "uk_performance_seats_id_performance", columnNames = ["id", "performance_id"]),
    ],
    indexes = [Index(name = "idx_performance_seats_holder", columnList = "holder_id, performance_id")],
)
class PerformanceSeatEntity(
    @Column(nullable = false)
    val performanceId: Long,

    @Column(nullable = false)
    val seatId: Long,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    val status: PerformanceSeatStatus,

    val holderId: Long?,

    val holdExpiresAt: LocalDateTime?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    fun statusAt(now: LocalDateTime): PerformanceSeatStatus =
        if (holdExpiresAt?.isBefore(now) == true) {
            PerformanceSeatStatus.AVAILABLE
        } else {
            status
        }

    init {
        val holdInfoPresent = holderId != null && holdExpiresAt != null
        val holdInfoAbsent = holderId == null && holdExpiresAt == null
        requireRule(if (status == PerformanceSeatStatus.HELD) holdInfoPresent else holdInfoAbsent) {
            "임시 배정 상태일 때만 임시 배정 사용자와 만료 시각이 있어야 합니다."
        }
    }

    companion object {
        val HOLD_DURATION: Duration = Duration.ofMinutes(5)
    }
}
