package io.github.hyeon0208.firstcome.show.application

import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceRepository
import io.github.hyeon0208.firstcome.show.application.dto.ShowResponse
import io.github.hyeon0208.firstcome.show.infrastructure.JpaShowRepository
import io.github.hyeon0208.firstcome.show.infrastructure.JpaShowSeatPriceRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.show.domain.VenueErrorCode
import io.github.hyeon0208.firstcome.show.infrastructure.JpaVenueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ShowReadService(
    private val venueRepository: JpaVenueRepository,
    private val showRepository: JpaShowRepository,
    private val showSeatPriceRepository: JpaShowSeatPriceRepository,
    private val performanceRepository: JpaPerformanceRepository,
) {
    @Transactional(readOnly = true)
    fun getShows(venueId: Long): List<ShowResponse> {
        if (!venueRepository.existsById(venueId)) {
            throw BusinessException(VenueErrorCode.VENUE_NOT_FOUND)
        }

        val shows = showRepository.findAllByVenueId(venueId)
        val showIds = shows.map { it.id }
        val seatPricesByShow = showSeatPriceRepository.findAllByShowIdIn(showIds).groupBy { it.showId }
        val performancesByShow = performanceRepository.findAllByShowIdInOrderByStartAt(showIds).groupBy { it.showId }

        return shows.map { show ->
            ShowResponse.of(show, seatPricesByShow[show.id].orEmpty(), performancesByShow[show.id].orEmpty())
        }
    }
}
