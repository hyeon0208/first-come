package io.github.hyeon0208.firstcome.performance.domain

import io.github.hyeon0208.firstcome.support.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Duration
import java.time.LocalDateTime

@Entity
@Table(name = "performance_participants")
class ParticipantEntity(
    @Column(nullable = false)
    val performanceId: Long,

    @Column(nullable = false)
    val participantNo: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    var preselectExpiresAt: LocalDateTime? = null
        protected set

    var holdExpiresAt: LocalDateTime? = null
        protected set

    // 아직 끝나지 않은 가선점 만료 시각. 없거나 지났으면 null이다.
    fun alivePreselectExpiresAt(now: LocalDateTime): LocalDateTime? =
        preselectExpiresAt?.takeIf { !it.isBefore(now) }

    // 가선점 시간은 첫 좌석을 누른 순간부터 흐른다. 좌석을 더 누르거나 전부 뺐다가 다시 골라도 늘지 않고,
    // 시간이 끝난 뒤 다시 고를 때만 새로 센다.
    fun startPreselect(now: LocalDateTime, preselectDuration: Duration): LocalDateTime =
        alivePreselectExpiresAt(now) ?: now.plus(preselectDuration).also { preselectExpiresAt = it }

    // 선택 완료가 성공한 순간부터 예매 가능 시간이 흐른다.
    fun hold(now: LocalDateTime, holdDuration: Duration): LocalDateTime {
        preselectExpiresAt = null
        return now.plus(holdDuration).also { holdExpiresAt = it }
    }

    // 좌석 선택 단계로 돌아가면 두 시계를 모두 지운다. 다시 고르는 첫 좌석부터 가선점 시간을 새로 센다.
    fun clearHold() {
        preselectExpiresAt = null
        holdExpiresAt = null
    }
}
