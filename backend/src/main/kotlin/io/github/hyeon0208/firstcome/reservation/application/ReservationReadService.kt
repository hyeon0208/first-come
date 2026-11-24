package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.performance.BookingPhase
import io.github.hyeon0208.firstcome.performance.ParticipantNo
import io.github.hyeon0208.firstcome.performance.application.BookingFacade
import io.github.hyeon0208.firstcome.performance.application.PerformanceReadService
import io.github.hyeon0208.firstcome.performance.application.PerformanceSeatReadService
import io.github.hyeon0208.firstcome.reservation.application.dto.QuoteResponse
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationSeatResponse
import io.github.hyeon0208.firstcome.reservation.domain.ReservationErrorCode
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationRepository
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationSeatRepository
import io.github.hyeon0208.firstcome.show.application.ShowReadService
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.error.requireRule
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationReadService(
    private val reservationRepository: JpaReservationRepository,
    private val reservationSeatRepository: JpaReservationSeatRepository,
    private val bookingFacade: BookingFacade,
    private val performanceReadService: PerformanceReadService,
    private val performanceSeatReadService: PerformanceSeatReadService,
    private val showReadService: ShowReadService,
    private val reservationProperties: ReservationProperties,
) {
    // 사용자가 선점한 좌석으로 결제할 금액(예매 정보 입력, 결제 화면).
    fun getQuote(userId: Long, performanceId: Long): QuoteResponse {
        val performance = bookingFacade.requireActive(userId, performanceId)
        val heldSeats = performanceSeatReadService.getParticipantSeats(performanceId, ParticipantNo.USER)
        requireRule(heldSeats.phase == BookingPhase.HELD || heldSeats.phase == BookingPhase.PAYING, ReservationErrorCode.HOLD_NOT_FOUND)
        val seats = showReadService.getSeats(performance.showId).filter { it.seatId in heldSeats.seatIds }
        return QuoteResponse.of(seats.map { ReservationSeatResponse.of(it, it.price) }, reservationProperties.feePerTicket)
    }

    // 사용자가 그 회차에서 확정한 예매(예매 완료 화면). 회차 데이터가 지워진 뒤에는 없다.
    @Transactional(readOnly = true)
    fun getConfirmedReservation(userId: Long, performanceId: Long): ReservationResponse {
        performanceReadService.findPerformance(userId, performanceId) ?: throw BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND)
        val reservation = reservationRepository.findByPerformanceIdAndParticipantNoAndStatus(performanceId, ParticipantNo.USER, ReservationStatus.CONFIRMED)
            ?: throw BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND)
        return getReservation(reservation.id)
    }

    @Transactional(readOnly = true)
    fun getReservation(reservationId: Long): ReservationResponse {
        val reservation = checkNotNull(reservationRepository.findByIdOrNull(reservationId)) { "예매가 없습니다. reservationId=$reservationId" }
        val performance = checkNotNull(performanceReadService.findPerformance(reservation.performanceId)) { "회차가 없습니다. performanceId=${reservation.performanceId}" }
        val seatsById = showReadService.getSeats(performance.showId).associateBy { it.seatId }
        val seats = reservationSeatRepository.findAllByReservationId(reservationId)
            .sortedBy { it.seatId }
            .map { ReservationSeatResponse.of(seatsById.getValue(it.seatId), it.price) }
        return ReservationResponse.of(reservation, seats)
    }
}
