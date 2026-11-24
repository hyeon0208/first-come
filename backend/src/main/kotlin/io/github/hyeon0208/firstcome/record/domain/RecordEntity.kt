package io.github.hyeon0208.firstcome.record.domain

import io.github.hyeon0208.firstcome.performance.Difficulty
import io.github.hyeon0208.firstcome.performance.PerformanceResult
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

// 회차가 끝난 뒤 남는 결과. 공연 이름은 그때의 글자로 남긴다(회차와 좌석 데이터는 지워진다).
@Entity
@Table(name = "records")
class RecordEntity(
    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val performanceId: Long,

    @Column(nullable = false, length = 100)
    val showTitle: String,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    val difficulty: Difficulty,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    val result: PerformanceResult,

    @Column(nullable = false)
    val elapsedSeconds: Long,

    @Column(nullable = false)
    val endedAt: LocalDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}
