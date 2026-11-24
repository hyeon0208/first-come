package io.github.hyeon0208.firstcome.reservation.application

import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceRepository
import io.github.hyeon0208.firstcome.performance.infrastructure.JpaPerformanceSeatRepository
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationResponse
import io.github.hyeon0208.firstcome.reservation.application.dto.ReservationSeatResponse
import io.github.hyeon0208.firstcome.reservation.domain.ReservationEntity
import io.github.hyeon0208.firstcome.reservation.domain.ReservationStatus
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationRepository
import io.github.hyeon0208.firstcome.reservation.infrastructure.JpaReservationSeatRepository
import io.github.hyeon0208.firstcome.show.infrastructure.JpaShowRepository
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.user.domain.UserErrorCode
import io.github.hyeon0208.firstcome.user.infrastructure.JpaUserRepository
import io.github.hyeon0208.firstcome.show.infrastructure.JpaSeatRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationReadService(
    private val reservationRepository: JpaReservationRepository,
    private val reservationSeatRepository: JpaReservationSeatRepository,
    private val performanceRepository: JpaPerformanceRepository,
    private val showRepository: JpaShowRepository,
    private val performanceSeatRepository: JpaPerformanceSeatRepository,
    private val seatRepository: JpaSeatRepository,
    private val userRepository: JpaUserRepository,
) {
    @Transactional(readOnly = true)
    fun getUserReservations(userId: Long): List<ReservationResponse> {
        if (!userRepository.existsById(userId)) {
            throw BusinessException(UserErrorCode.USER_NOT_FOUND)
        }
        return toResponses(reservationRepository.findAllByUserIdAndStatusOrderByIdDesc(userId, ReservationStatus.CONFIRMED))
    }

    @Transactional(readOnly = true)
    fun getReservation(reservationId: Long): ReservationResponse {
        val reservation = checkNotNull(reservationRepository.findByIdOrNull(reservationId)) { "예약이 없습니다. reservationId=$reservationId" }
        return toResponses(listOf(reservation)).single()
    }

    private fun toResponses(reservations: List<ReservationEntity>): List<ReservationResponse> {
        if (reservations.isEmpty()) {
            return emptyList()
        }
        val reservationSeatsByReservationId = reservationSeatRepository.findAllByReservationIdIn(reservations.map { it.id })
            .groupBy { it.reservationId }
        val seatIdByPerformanceSeatId = performanceSeatRepository.findAllById(reservationSeatsByReservationId.values.flatten().map { it.performanceSeatId })
            .associate { it.id to it.seatId }
        val seatsById = seatRepository.findAllById(seatIdByPerformanceSeatId.values).associateBy { it.id }
        val performancesById = performanceRepository.findAllById(reservations.map { it.performanceId }.toSet()).associateBy { it.id }
        val showsById = showRepository.findAllById(performancesById.values.map { it.showId }.toSet()).associateBy { it.id }

        return reservations.map { reservation ->
            val performance = performancesById.getValue(reservation.performanceId)
            val seats = reservationSeatsByReservationId.getValue(reservation.id).sortedBy { it.performanceSeatId }.map { reservationSeat ->
                ReservationSeatResponse.of(reservationSeat, seatsById.getValue(seatIdByPerformanceSeatId.getValue(reservationSeat.performanceSeatId)))
            }
            ReservationResponse.of(reservation, showsById.getValue(performance.showId), performance, seats)
        }
    }
}
