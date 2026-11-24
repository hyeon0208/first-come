package io.github.hyeon0208.firstcome.performance.domain

import io.github.hyeon0208.firstcome.performance.Difficulty
import io.github.hyeon0208.firstcome.performance.PerformanceResult
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
import java.time.Duration
import java.time.LocalDateTime
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "performances")
class PerformanceEntity(
    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val showId: Long,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    val difficulty: Difficulty,

    // 가상 경쟁자가 들어오는 시각과 고르는 좌석을 정하는 난수 씨앗. 같은 씨앗이면 같은 순서가 나온다.
    @Column(nullable = false)
    val seed: Long,

    @Column(nullable = false)
    val openAt: LocalDateTime,

    @Column(nullable = false)
    val startAt: LocalDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    var status: PerformanceStatus = PerformanceStatus.ACTIVE
        protected set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 10)
    var result: PerformanceResult? = null
        protected set

    // 진행 중일 때만 사용자 id다. 유일 제약으로 한 사용자의 진행 중 회차를 하나로 막는다.
    var activeUserId: Long? = userId
        protected set

    var enteredAt: LocalDateTime? = null
        protected set

    var seatSelectionExpiresAt: LocalDateTime? = null
        protected set

    var endedAt: LocalDateTime? = null
        protected set

    val isActive: Boolean
        get() = status == PerformanceStatus.ACTIVE

    val isEntered: Boolean
        get() = enteredAt != null

    fun isOpenAt(now: LocalDateTime): Boolean =
        !now.isBefore(openAt)

    fun isStartedAt(now: LocalDateTime): Boolean =
        !now.isBefore(startAt)

    // 예매 화면에 들어온 순간부터 좌석 선택 시간이 흐른다. 다시 들어와도 시계는 처음 그대로다.
    fun enter(now: LocalDateTime, seatSelectionDuration: Duration) {
        requireRule(isOpenAt(now), PerformanceErrorCode.NOT_OPEN_YET)
        if (enteredAt == null) {
            enteredAt = now
            seatSelectionExpiresAt = now.plus(seatSelectionDuration)
        }
    }

    fun isSeatSelectionOverAt(now: LocalDateTime): Boolean =
        seatSelectionExpiresAt?.isBefore(now) == true

    fun isEnterGraceOverAt(now: LocalDateTime, enterGrace: Duration): Boolean =
        enteredAt == null && openAt.plus(enterGrace).isBefore(now)

    fun end(result: PerformanceResult, now: LocalDateTime) {
        check(isActive) { "진행 중인 회차만 끝낼 수 있습니다. performanceId=$id status=$status" }
        status = PerformanceStatus.ENDED
        this.result = result
        endedAt = now
        activeUserId = null
    }

    // 예매 오픈 시각부터 끝난 순간까지. 오픈 전에 끝나면 0초다.
    fun elapsedSeconds(): Long =
        Duration.between(openAt, checkNotNull(endedAt) { "끝나지 않은 회차입니다. performanceId=$id" }).seconds.coerceAtLeast(0)
}
