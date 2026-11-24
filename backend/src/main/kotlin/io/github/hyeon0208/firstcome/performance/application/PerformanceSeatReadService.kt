package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceSeatResponse
import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceSeatsResponse
import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceRepository
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceSeatRepository
import io.github.hyeon0208.firstcome.show.domain.ShowSeatPrices
import io.github.hyeon0208.firstcome.show.infrastructure.JpaShowRepository
import io.github.hyeon0208.firstcome.show.infrastructure.JpaShowSeatPriceRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.show.infrastructure.JpaSeatRepository
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PerformanceSeatReadService(
    private val showRepository: JpaShowRepository,
    private val performanceRepository: JpaPerformanceRepository,
    private val showSeatPriceRepository: JpaShowSeatPriceRepository,
    private val seatRepository: JpaSeatRepository,
    private val performanceSeatRepository: JpaPerformanceSeatRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getPerformanceSeats(venueId: Long, showId: Long, performanceId: Long): PerformanceSeatsResponse {
        val show = showRepository.findByIdAndVenueId(showId, venueId)
            ?: throw BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND)
        val performance = performanceRepository.findByIdAndShowId(performanceId, showId)
            ?: throw BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND)

        val seatsById = seatRepository.findAllByVenueId(show.venueId).associateBy { it.id }
        val prices = ShowSeatPrices(show.id, showSeatPriceRepository.findAllByShowId(show.id))
        val now = LocalDateTime.now(clock)

        val seats = performanceSeatRepository.findAllByPerformanceIdOrderById(performance.id).map { performanceSeat ->
            val seat = seatsById.getValue(performanceSeat.seatId)
            PerformanceSeatResponse.of(performanceSeat, seat, prices.priceOf(seat.grade), performanceSeat.statusAt(now))
        }
        return PerformanceSeatsResponse.of(show, performance, seats)
    }
}
