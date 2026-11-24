package io.github.hyeon0208.firstcome.performance.application

import io.github.hyeon0208.firstcome.performance.BookingPhase
import io.github.hyeon0208.firstcome.performance.ParticipantNo
import io.github.hyeon0208.firstcome.performance.PerformanceResult
import io.github.hyeon0208.firstcome.performance.application.dto.BookingStateResponse
import io.github.hyeon0208.firstcome.performance.application.dto.HoldResponse
import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceCheckResponse
import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceResponse
import io.github.hyeon0208.firstcome.performance.application.dto.PreselectResult
import io.github.hyeon0208.firstcome.performance.application.dto.SeatStatusesResponse
import io.github.hyeon0208.firstcome.performance.application.dto.ValidatePricesRequest
import io.github.hyeon0208.firstcome.performance.domain.PerformanceErrorCode
import io.github.hyeon0208.firstcome.show.application.ShowReadService
import io.github.hyeon0208.firstcome.support.error.BusinessException
import io.github.hyeon0208.firstcome.support.error.requireRule
import org.springframework.stereotype.Service

// 사용자가 예매 화면에서 보내는 요청의 창구. 요청마다 그 회차가 그 사용자의 진행 중 회차인지 먼저 확인하고(제한 시간이 지났으면 여기서 끝난다),
// 좌석을 바꾸는 일은 가상 경쟁자와 같은 PerformanceSeatWriteService에 맡긴다.
// 회차를 끝내는 일과 요청을 거절하는 일이 한 트랜잭션에 묶여 함께 롤백되지 않도록, 이 클래스에는 트랜잭션을 걸지 않는다.
@Service
class BookingFacade(
    private val performanceWriteService: PerformanceWriteService,
    private val performanceSeatWriteService: PerformanceSeatWriteService,
    private val performanceSeatReadService: PerformanceSeatReadService,
    private val showReadService: ShowReadService,
) {
    // 진행 중인 회차가 아니면 거절한다. 다른 모듈이 사용자의 요청을 받을 때도 이 확인을 거친다.
    fun requireActive(userId: Long, performanceId: Long): PerformanceResponse {
        val check = performanceWriteService.checkIn(userId, performanceId)
        return when (check.state) {
            PerformanceCheckResponse.State.ACTIVE -> checkNotNull(check.performance)
            PerformanceCheckResponse.State.TIMED_OUT -> throw BusinessException(PerformanceErrorCode.TIME_OVER)
            PerformanceCheckResponse.State.GONE -> throw BusinessException(PerformanceErrorCode.SESSION_EXPIRED)
        }
    }

    // 예매 화면에 들어온다. 좌석 선택 시간이 이때 시작된다.
    fun enter(userId: Long, performanceId: Long): BookingStateResponse {
        requireActive(userId, performanceId)
        return performanceSeatReadService.getBookingState(performanceWriteService.enter(performanceId))
    }

    fun getState(userId: Long, performanceId: Long): BookingStateResponse =
        performanceSeatReadService.getBookingState(requireEntered(userId, performanceId))

    // 좌석 상태. 남은 좌석이 없고 내 좌석도 없으면 회차를 매진으로 끝낸다.
    fun getSeatStatuses(userId: Long, performanceId: Long): SeatStatusesResponse {
        requireEntered(userId, performanceId)
        val statuses = performanceSeatReadService.getSeatStatuses(performanceId, ParticipantNo.USER)
        if (statuses.soldOut && statuses.mySeatIds.isEmpty()) {
            performanceWriteService.end(performanceId, PerformanceResult.SOLD_OUT)
        }
        return statuses
    }

    fun preselect(userId: Long, performanceId: Long, seatId: Long): BookingStateResponse {
        val performance = requireEntered(userId, performanceId)
        when (performanceSeatWriteService.preselect(performanceId, ParticipantNo.USER, seatId)) {
            PreselectResult.SELECTED -> Unit
            PreselectResult.TAKEN -> throw BusinessException(PerformanceErrorCode.SEAT_TAKEN)
            PreselectResult.SOLD_OUT -> {
                if (performanceSeatReadService.getParticipantSeats(performanceId, ParticipantNo.USER).seatIds.isNotEmpty()) {
                    throw BusinessException(PerformanceErrorCode.SEAT_TAKEN)
                }
                performanceWriteService.end(performanceId, PerformanceResult.SOLD_OUT)
                throw BusinessException(PerformanceErrorCode.SOLD_OUT)
            }
        }
        return performanceSeatReadService.getBookingState(performance)
    }

    fun unpreselect(userId: Long, performanceId: Long, seatId: Long): BookingStateResponse {
        val performance = requireEntered(userId, performanceId)
        performanceSeatWriteService.unpreselect(performanceId, ParticipantNo.USER, seatId)
        return performanceSeatReadService.getBookingState(performance)
    }

    fun unpreselectAll(userId: Long, performanceId: Long): BookingStateResponse {
        val performance = requireEntered(userId, performanceId)
        performanceSeatWriteService.unpreselectAll(performanceId, ParticipantNo.USER)
        return performanceSeatReadService.getBookingState(performance)
    }

    // 선택 완료. 예매 가능 시간이 이때 시작된다.
    fun hold(userId: Long, performanceId: Long): HoldResponse {
        val performance = requireEntered(userId, performanceId)
        val held = performanceSeatWriteService.hold(performanceId, ParticipantNo.USER)
        return HoldResponse.of(held, performanceSeatReadService.getBookingState(performance))
    }

    // 가격 단계에서 좌석 단계로 돌아간다. 좌석 선택 시간이 이미 끝났으면 돌아갈 수 없다.
    fun releaseHold(userId: Long, performanceId: Long): BookingStateResponse {
        val performance = requireEntered(userId, performanceId)
        val isSeatSelectionOver = performance.seatSelectionExpiresAt?.isBefore(performance.serverNow) == true
        requireRule(!isSeatSelectionOver, PerformanceErrorCode.BACK_BLOCKED)
        performanceSeatWriteService.releaseHold(performanceId, ParticipantNo.USER)
        return performanceSeatReadService.getBookingState(performance)
    }

    // 가격 종류별 수량이 선점한 좌석의 등급별 수와 같아야 다음 단계로 간다.
    fun validatePrices(userId: Long, performanceId: Long, request: ValidatePricesRequest) {
        val performance = requireEntered(userId, performanceId)
        val seats = performanceSeatReadService.getParticipantSeats(performanceId, ParticipantNo.USER)
        requireRule(seats.phase == BookingPhase.HELD, PerformanceErrorCode.HOLD_NOT_FOUND)
        val gradeBySeatId = showReadService.getSeats(performance.showId).associate { it.seatId to it.grade }
        val heldCountByGrade = seats.seatIds.groupingBy { gradeBySeatId.getValue(it) }.eachCount()
        val requestedCountByGrade = request.quantities.filter { it.quantity != 0 }.groupBy({ it.grade }, { it.quantity }).mapValues { it.value.sum() }
        requireRule(heldCountByGrade == requestedCountByGrade, PerformanceErrorCode.PRICE_COUNT_MISMATCH)
    }

    private fun requireEntered(userId: Long, performanceId: Long): PerformanceResponse =
        requireActive(userId, performanceId).also { requireRule(it.enteredAt != null, PerformanceErrorCode.NOT_ENTERED) }
}
