package io.github.hyeon0208.firstcome.performance.infrastructure

import io.github.hyeon0208.firstcome.performance.domain.ParticipantEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface JpaParticipantRepository : JpaRepository<ParticipantEntity, Long> {
    fun findByPerformanceIdAndParticipantNo(performanceId: Long, participantNo: Int): ParticipantEntity?

    // 한 참가자의 요청을 한 번에 하나씩 처리하려고 참가자 행을 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ParticipantEntity p where p.performanceId = :performanceId and p.participantNo = :participantNo")
    fun findForUpdate(performanceId: Long, participantNo: Int): ParticipantEntity?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ParticipantEntity p where p.performanceId = :performanceId")
    fun deleteAllByPerformanceId(performanceId: Long): Int
}
