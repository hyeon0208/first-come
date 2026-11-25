package io.github.hyeon0208.firstcome.competitor.application

import io.github.hyeon0208.firstcome.competitor.domain.SeatPicker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

// 한 회차에서 움직이는 가상 경쟁자들의 상태. 서버 메모리에만 있다.
// 예약을 거는 쪽(회차 생성 뒤)과 멈추는 쪽(회차 종료 뒤)이 다른 스레드라, 예약 목록은 잠가서 다룬다.
class CompetitorRun(
    val performanceId: Long,
    val seatPicker: SeatPicker,
) {
    // 가상 경쟁자들이 팔린 것으로 아는 좌석. 자기들이 잡은 좌석과, 눌렀다가 거절당한 좌석이다.
    // 실제 상태와 조금 어긋날 수 있다(사용자가 놓은 좌석 등). 남은 좌석이 없어 보이면 서버에서 다시 받아 바꾼다.
    val takenSeatIds: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    fun replaceTakenSeats(seatIds: Collection<Long>) {
        takenSeatIds.retainAll(seatIds.toSet())
        takenSeatIds.addAll(seatIds)
    }

    private val scheduled = mutableListOf<ScheduledFuture<*>>()

    @Volatile
    var isStopped: Boolean = false
        private set

    // 이미 멈춘 뒤에 걸린 예약은 바로 취소한다.
    @Synchronized
    fun add(future: ScheduledFuture<*>) {
        if (isStopped) future.cancel(false) else scheduled += future
    }

    @Synchronized
    fun stop() {
        isStopped = true
        scheduled.forEach { it.cancel(false) }
        scheduled.clear()
    }
}
