package io.github.hyeon0208.firstcome.performance.domain

import io.github.hyeon0208.firstcome.support.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "performances",
    uniqueConstraints = [UniqueConstraint(name = "uk_performances_show_start", columnNames = ["show_id", "start_at"])],
)
class PerformanceEntity(
    @Column(nullable = false)
    val showId: Long,

    @Column(nullable = false)
    val startAt: LocalDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    fun isStartedAt(now: LocalDateTime): Boolean =
        !now.isBefore(startAt)
}
