package io.github.hyeon0208.firstcome.competitor.application

import io.github.hyeon0208.firstcome.competitor.domain.CompetitorPlan
import io.github.hyeon0208.firstcome.competitor.domain.CompetitorPlanner
import io.github.hyeon0208.firstcome.competitor.domain.SeatPicker
import io.github.hyeon0208.firstcome.performance.PerformanceCreated
import io.github.hyeon0208.firstcome.performance.PerformanceEnded
import io.github.hyeon0208.firstcome.performance.application.BookingProperties
import io.github.hyeon0208.firstcome.performance.application.PerformanceReadService
import io.github.hyeon0208.firstcome.performance.application.PerformanceSeatWriteService
import io.github.hyeon0208.firstcome.performance.application.dto.PreselectResult
import io.github.hyeon0208.firstcome.reservation.application.ReservationFacade
import io.github.hyeon0208.firstcome.show.application.ShowReadService
import io.github.hyeon0208.firstcome.show.application.dto.SeatResponse
import io.github.hyeon0208.firstcome.support.error.BusinessException
import jakarta.annotation.PreDestroy
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

// 가상 경쟁자를 움직인다. 화면 연출이 아니라, 사용자의 요청이 타는 것과 같은 서비스(가선점 → 선점 → 결제 → 확정)를 부른다.
// 회차가 만들어지면 계획대로 예약해 두고, 예매 오픈 시각부터 한 명씩 가상 스레드에서 실행한다. 회차가 끝나면 멈춘다.
@Component
class CompetitorRunner(
    private val competitorProperties: CompetitorProperties,
    private val bookingProperties: BookingProperties,
    private val showReadService: ShowReadService,
    private val performanceReadService: PerformanceReadService,
    private val performanceSeatWriteService: PerformanceSeatWriteService,
    private val reservationFacade: ReservationFacade,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    // 취소한 예약은 큐에서 바로 뺀다. 그러지 않으면 원래 실행 시각까지 끝난 회차의 상태를 붙잡는다.
    private val scheduler = ScheduledThreadPoolExecutor(1, Thread.ofPlatform().name("competitor-scheduler").daemon().factory())
        .apply { removeOnCancelPolicy = true }
    private val workers = Executors.newVirtualThreadPerTaskExecutor()
    private val runs = ConcurrentHashMap<Long, CompetitorRun>()

    // 회차를 만든 트랜잭션이 커밋된 뒤에 시작한다. 그 전에는 회차와 좌석이 다른 연결에서 보이지 않는다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun start(event: PerformanceCreated) {
        if (!competitorProperties.enabled) {
            return
        }
        val setting = competitorProperties.settingOf(event.difficulty)
        val plans = CompetitorPlanner.plan(event.seed, setting.count, setting.arrivalWindow, bookingProperties.maxSeats)
        val run = CompetitorRun(event.performanceId, SeatPicker(showReadService.getSeats(event.showId)))
        runs[event.performanceId] = run
        val untilOpen = Duration.between(LocalDateTime.now(clock), event.openAt)
        plans.forEach { plan ->
            val delayMillis = untilOpen.plus(plan.arrivalOffset).toMillis().coerceAtLeast(0)
            run.add(scheduler.schedule({ workers.execute { act(run, plan) } }, delayMillis, TimeUnit.MILLISECONDS))
        }
        // 예약을 거는 사이 회차가 이미 끝났을 수 있다(종료 알림이 먼저 지나간 경우). 그러면 여기서 멈춘다.
        if (performanceReadService.findPerformance(event.performanceId)?.active != true) {
            runs.remove(event.performanceId)?.stop()
            return
        }
        logger.info(
            "가상 경쟁자 예약 performanceId={} difficulty={} count={} arrivalWindow={}",
            event.performanceId,
            event.difficulty,
            plans.size,
            setting.arrivalWindow,
        )
    }

    // 회차를 끝낸 트랜잭션이 커밋된 뒤에 멈춘다. 종료가 롤백되면 가상 경쟁자는 계속 움직여야 한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun stop(event: PerformanceEnded) {
        runs.remove(event.performanceId)?.stop()
    }

    @PreDestroy
    fun shutdown() {
        scheduler.shutdownNow()
        workers.shutdownNow()
    }

    // 가상 경쟁자 한 명의 예매. 좌석을 눌러 거절당하면 다른 좌석을 고르고, 하나라도 잡았으면 결제까지 간다.
    private fun act(run: CompetitorRun, plan: CompetitorPlan) {
        try {
            if (run.isStopped) {
                return
            }
            val random = Random(plan.pickSeed)
            performanceSeatWriteService.join(run.performanceId, plan.participantNo)
            val selected = pressSeats(run, plan, random)
            if (selected.isEmpty() || run.isStopped) {
                return
            }
            if (!performanceSeatWriteService.hold(run.performanceId, plan.participantNo).isHeld) {
                return
            }
            pause(random, competitorProperties.checkoutDelayMin, competitorProperties.checkoutDelayMax)
            if (!run.isStopped) {
                reservationFacade.reserve(run.performanceId, plan.participantNo, "competitor-${plan.participantNo}")
            }
        } catch (exception: BusinessException) {
            // 회차가 먼저 끝나 지워졌거나 결제가 거절된 경우다. 사용자가 받는 것과 같은 거절이다.
            logger.debug("가상 경쟁자 중단 performanceId={} participantNo={} code={}", run.performanceId, plan.participantNo, exception.errorCode.name)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (exception: Exception) {
            if (!run.isStopped) {
                logger.warn("가상 경쟁자 실패 performanceId={} participantNo={}", run.performanceId, plan.participantNo, exception)
            }
        }
    }

    private fun pressSeats(run: CompetitorRun, plan: CompetitorPlan, random: Random): List<SeatResponse> {
        val selected = mutableListOf<SeatResponse>()
        var rejections = 0
        while (selected.size < plan.wantedSeats && rejections < MAX_REJECTIONS && !run.isStopped) {
            val believedAvailable = run.seatPicker.pick(random, run.takenSeatIds, selected)
            val seat = believedAvailable ?: run.seatPicker.pickAny(random)
            val result = performanceSeatWriteService.preselect(run.performanceId, plan.participantNo, seat.seatId)
            run.takenSeatIds += seat.seatId
            when (result) {
                PreselectResult.SELECTED -> if (seat !in selected) selected += seat
                PreselectResult.TAKEN -> rejections++
                PreselectResult.SOLD_OUT -> break
            }
            // 팔린 줄 아는 좌석뿐일 때는 하나만 눌러 보고 그만둔다.
            if (believedAvailable == null && result != PreselectResult.SELECTED) {
                break
            }
            pause(random, competitorProperties.pressIntervalMin, competitorProperties.pressIntervalMax)
        }
        return selected
    }

    private fun pause(random: Random, min: Duration, max: Duration) {
        val millis = if (max > min) random.nextLong(min.toMillis(), max.toMillis() + 1) else min.toMillis()
        if (millis > 0) {
            Thread.sleep(millis)
        }
    }

    companion object {
        private const val MAX_REJECTIONS = 5
    }
}
