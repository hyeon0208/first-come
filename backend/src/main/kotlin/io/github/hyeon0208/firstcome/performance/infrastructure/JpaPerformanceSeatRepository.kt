package io.github.hyeon0208.firstcome.performance.infrastructure

import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatEntity
import io.github.hyeon0208.firstcome.performance.domain.PerformanceSeatStatus
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

// 좌석 상태는 "지금 이 상태이고, 이 참가자의 것이고, 만료되지 않았을 때만"을 WHERE에 넣은 UPDATE로만 바꾸고,
// 바뀐 행 수로 성공을 판정한다. UPDATE는 영속성 컨텍스트를 비우므로, 그 전에 읽은 엔티티는 뒤에서 고치지 않는다.
interface JpaPerformanceSeatRepository : JpaRepository<PerformanceSeatEntity, Long> {
    fun findAllByPerformanceId(performanceId: Long): List<PerformanceSeatEntity>

    fun findAllByPerformanceIdAndParticipantNo(performanceId: Long, participantNo: Int): List<PerformanceSeatEntity>

    fun existsByPerformanceIdAndSeatId(performanceId: Long, seatId: Long): Boolean

    fun existsByPerformanceIdAndParticipantNoAndStatus(performanceId: Long, participantNo: Int, status: PerformanceSeatStatus): Boolean

    // 아무도 가져가지 않은 좌석 수. 0이면 매진이다.
    @Query(
        """
        select count(s) from PerformanceSeatEntity s
        where s.performanceId = :performanceId
          and (s.status = AVAILABLE or (s.status in (PRESELECTED, HELD) and s.expiresAt < :now))
        """,
    )
    fun countAvailable(performanceId: Long, now: LocalDateTime): Long

    // 가선점: 선택 가능이거나 만료된 가선점·선점일 때만 가져간다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity s
        set s.status = PRESELECTED, s.participantNo = :participantNo, s.expiresAt = :expiresAt, s.updatedAt = :now
        where s.performanceId = :performanceId and s.seatId = :seatId
          and (s.status = AVAILABLE or (s.status in (PRESELECTED, HELD) and s.expiresAt < :now))
        """,
    )
    fun preselect(performanceId: Long, seatId: Long, participantNo: Int, expiresAt: LocalDateTime, now: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity s
        set s.status = AVAILABLE, s.participantNo = null, s.expiresAt = null, s.updatedAt = :now
        where s.performanceId = :performanceId and s.participantNo = :participantNo and s.seatId = :seatId and s.status = PRESELECTED
        """,
    )
    fun unpreselect(performanceId: Long, participantNo: Int, seatId: Long, now: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity s
        set s.status = AVAILABLE, s.participantNo = null, s.expiresAt = null, s.updatedAt = :now
        where s.performanceId = :performanceId and s.participantNo = :participantNo and s.status = PRESELECTED
        """,
    )
    fun unpreselectAll(performanceId: Long, participantNo: Int, now: LocalDateTime): Int

    // 가선점 시간이 끝난 내 좌석을 정리한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity s
        set s.status = AVAILABLE, s.participantNo = null, s.expiresAt = null, s.updatedAt = :now
        where s.performanceId = :performanceId and s.participantNo = :participantNo and s.status = PRESELECTED and s.expiresAt < :now
        """,
    )
    fun releaseLapsedPreselects(performanceId: Long, participantNo: Int, now: LocalDateTime): Int

    // 선점: 만료되지 않은 내 가선점 좌석 전체를 한 번에 바꾼다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity s
        set s.status = HELD, s.expiresAt = :holdExpiresAt, s.updatedAt = :now
        where s.performanceId = :performanceId and s.participantNo = :participantNo and s.status = PRESELECTED and s.expiresAt >= :now
        """,
    )
    fun hold(performanceId: Long, participantNo: Int, holdExpiresAt: LocalDateTime, now: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity s
        set s.status = AVAILABLE, s.participantNo = null, s.expiresAt = null, s.updatedAt = :now
        where s.performanceId = :performanceId and s.participantNo = :participantNo and s.status = HELD
        """,
    )
    fun releaseHeld(performanceId: Long, participantNo: Int, now: LocalDateTime): Int

    // 결제 중: 결제를 부르기 전에 만료되지 않은 내 선점 좌석을 잠근다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity s
        set s.status = PAYING, s.updatedAt = :now
        where s.performanceId = :performanceId and s.participantNo = :participantNo and s.status = HELD and s.expiresAt >= :now
        """,
    )
    fun startPaying(performanceId: Long, participantNo: Int, now: LocalDateTime): Int

    // 예매 완료: 결제가 승인된 뒤, 결제 중이고 예매 가능 시간이 남았을 때만 확정한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity s
        set s.status = RESERVED, s.expiresAt = null, s.updatedAt = :now
        where s.performanceId = :performanceId and s.participantNo = :participantNo and s.status = PAYING and s.expiresAt >= :now
        """,
    )
    fun reserve(performanceId: Long, participantNo: Int, now: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity s
        set s.status = HELD, s.updatedAt = :now
        where s.performanceId = :performanceId and s.participantNo = :participantNo and s.status = PAYING
        """,
    )
    fun revertPaying(performanceId: Long, participantNo: Int, now: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PerformanceSeatEntity s
        set s.status = AVAILABLE, s.participantNo = null, s.expiresAt = null, s.updatedAt = :now
        where s.performanceId = :performanceId and s.participantNo = :participantNo and s.status = PAYING
        """,
    )
    fun releasePaying(performanceId: Long, participantNo: Int, now: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PerformanceSeatEntity s where s.performanceId = :performanceId")
    fun deleteAllByPerformanceId(performanceId: Long): Int
}
