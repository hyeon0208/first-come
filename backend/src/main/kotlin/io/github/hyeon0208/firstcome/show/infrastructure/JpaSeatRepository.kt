package io.github.hyeon0208.firstcome.show.infrastructure

import io.github.hyeon0208.firstcome.show.domain.SeatEntity
import org.springframework.data.jpa.repository.JpaRepository

interface JpaSeatRepository : JpaRepository<SeatEntity, Long> {
    fun findAllByVenueId(venueId: Long): List<SeatEntity>
}
