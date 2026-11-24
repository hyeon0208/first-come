package io.github.hyeon0208.firstcome.show.infrastructure

import io.github.hyeon0208.firstcome.show.domain.ShowSeatPriceEntity
import org.springframework.data.jpa.repository.JpaRepository

interface JpaShowSeatPriceRepository : JpaRepository<ShowSeatPriceEntity, Long> {
    fun findAllByShowIdInOrderById(showIds: Collection<Long>): List<ShowSeatPriceEntity>
}
