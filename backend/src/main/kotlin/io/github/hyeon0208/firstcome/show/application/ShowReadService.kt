package io.github.hyeon0208.firstcome.show.application

import io.github.hyeon0208.firstcome.show.application.dto.SeatMapResponse
import io.github.hyeon0208.firstcome.show.application.dto.SeatResponse
import io.github.hyeon0208.firstcome.show.application.dto.ShowResponse
import io.github.hyeon0208.firstcome.show.domain.ShowErrorCode
import io.github.hyeon0208.firstcome.show.infrastructure.JpaSeatRepository
import io.github.hyeon0208.firstcome.show.infrastructure.JpaShowRepository
import io.github.hyeon0208.firstcome.show.infrastructure.JpaShowSeatPriceRepository
import io.github.hyeon0208.firstcome.show.infrastructure.JpaVenueRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Service

@Service
class ShowReadService(
    private val showRepository: JpaShowRepository,
    private val venueRepository: JpaVenueRepository,
    private val showSeatPriceRepository: JpaShowSeatPriceRepository,
    private val seatRepository: JpaSeatRepository,
) {
    // 공연과 좌석은 실행 중에 바뀌지 않는 고정 데이터다. 좌석 1,008개를 요청마다 다시 읽지 않도록 공연별로 한 번만 읽어 둔다.
    private val seatsByShowId = ConcurrentHashMap<Long, List<SeatResponse>>()

    fun getShows(): List<ShowResponse> {
        val shows = showRepository.findAll().sortedBy { it.id }
        val venuesById = venueRepository.findAllById(shows.map { it.venueId }.toSet()).associateBy { it.id }
        val pricesByShowId = showSeatPriceRepository.findAllByShowIdInOrderById(shows.map { it.id }).groupBy { it.showId }
        return shows.map { ShowResponse.of(it, venuesById.getValue(it.venueId), pricesByShowId[it.id].orEmpty()) }
    }

    fun getShow(showId: Long): ShowResponse =
        getShows().find { it.id == showId } ?: throw BusinessException(ShowErrorCode.SHOW_NOT_FOUND)

    fun getSeatMap(showId: Long): SeatMapResponse =
        SeatMapResponse.of(getShow(showId), getSeats(showId))

    fun getSeats(showId: Long): List<SeatResponse> =
        seatsByShowId.getOrPut(showId) {
            val show = showRepository.findById(showId).orElseThrow { BusinessException(ShowErrorCode.SHOW_NOT_FOUND) }
            val priceByGrade = showSeatPriceRepository.findAllByShowIdInOrderById(listOf(showId)).associateBy { it.grade }
            seatRepository.findAllByVenueIdOrderById(show.venueId).map { SeatResponse.of(it, priceByGrade.getValue(it.grade)) }
        }
}
