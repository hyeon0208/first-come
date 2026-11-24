package io.github.hyeon0208.firstcome.performance.domain

import io.github.hyeon0208.firstcome.support.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

// 상태는 조건부 UPDATE로만 바꾼다(JpaPerformanceSeatRepository). 이 엔티티는 읽는 용도다.
@Entity
@Table(name = "performance_seats")
class PerformanceSeatEntity(
    @Column(nullable = false)
    val performanceId: Long,

    @Column(nullable = false)
    val seatId: Long,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 12)
    val status: PerformanceSeatStatus,

    val participantNo: Int?,

    val expiresAt: LocalDateTime?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    // 누군가 가져간 좌석인가. 가선점과 선점은 만료 시각이 지나면 풀린 것으로 본다.
    // 결제 중인 좌석은 만료 시각이 지나도 확정 단계가 결과를 정할 때까지 풀리지 않는다.
    // 같은 판정이 조건부 UPDATE의 WHERE(JpaPerformanceSeatRepository.preselect)에도 있다.
    fun isTakenAt(now: LocalDateTime): Boolean =
        when (status) {
            PerformanceSeatStatus.AVAILABLE -> false
            PerformanceSeatStatus.PRESELECTED, PerformanceSeatStatus.HELD -> expiresAt?.isBefore(now) == false
            PerformanceSeatStatus.PAYING, PerformanceSeatStatus.RESERVED -> true
        }
}
