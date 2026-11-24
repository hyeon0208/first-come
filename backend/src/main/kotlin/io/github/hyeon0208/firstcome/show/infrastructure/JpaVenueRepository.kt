package io.github.hyeon0208.firstcome.show.infrastructure

import io.github.hyeon0208.firstcome.show.domain.VenueEntity
import org.springframework.data.jpa.repository.JpaRepository

interface JpaVenueRepository : JpaRepository<VenueEntity, Long>
