package io.github.hyeon0208.firstcome.show.infrastructure

import io.github.hyeon0208.firstcome.show.domain.ShowEntity
import org.springframework.data.jpa.repository.JpaRepository

interface JpaShowRepository : JpaRepository<ShowEntity, Long> {
    fun findAllByVenueId(venueId: Long): List<ShowEntity>

    fun findByIdAndVenueId(id: Long, venueId: Long): ShowEntity?
}
