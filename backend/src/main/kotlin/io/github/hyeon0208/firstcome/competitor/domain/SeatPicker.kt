package io.github.hyeon0208.firstcome.competitor.domain

import io.github.hyeon0208.firstcome.show.application.dto.SeatResponse
import kotlin.math.abs
import kotlin.random.Random

// 가상 경쟁자가 다음에 누를 좌석을 고른다.
// - 첫 좌석: 아직 안 팔린 것으로 아는 좌석 중에서 무작위로 고르되, 무대에 가까울수록 고를 확률이 높다.
// - 그다음 좌석: 방금 잡은 좌석과 같은 열에서 가장 가까운 자리를 먼저 노린다(일행이 나란히 앉으려는 것).
class SeatPicker(
    private val seats: List<SeatResponse>,
) {
    fun pick(random: Random, takenSeatIds: Set<Long>, selected: List<SeatResponse>): SeatResponse? {
        val candidates = seats.filter { it.seatId !in takenSeatIds }
        if (candidates.isEmpty()) {
            return null
        }
        return nearestInSameRow(candidates, selected.lastOrNull()) ?: pickByWeight(random, candidates)
    }

    private fun nearestInSameRow(candidates: List<SeatResponse>, last: SeatResponse?): SeatResponse? {
        if (last == null) {
            return null
        }
        return candidates
            .filter { it.floor == last.floor && it.block == last.block && it.rowNo == last.rowNo }
            .minByOrNull { abs(it.seatNo - last.seatNo) }
            ?.takeIf { abs(it.seatNo - last.seatNo) <= MAX_NEIGHBOR_GAP }
    }

    private fun pickByWeight(random: Random, candidates: List<SeatResponse>): SeatResponse {
        var point = random.nextDouble(candidates.sumOf(::weightOf))
        for (seat in candidates) {
            point -= weightOf(seat)
            if (point < 0) {
                return seat
            }
        }
        return candidates.last()
    }

    companion object {
        private const val MAX_NEIGHBOR_GAP = 2
        private const val ROWS_PER_FLOOR_ONE_BLOCK = 12
        private val FRONT_BLOCKS = setOf("A", "B")

        // 무대에서 떨어진 정도를 열 수로 센다. 1층 A·B가 무대 앞, C·D가 그 뒤, 2층이 가장 멀다.
        fun stageDistanceOf(seat: SeatResponse): Int =
            when {
                seat.floor == 1 && seat.block in FRONT_BLOCKS -> seat.rowNo
                seat.floor == 1 -> ROWS_PER_FLOOR_ONE_BLOCK + seat.rowNo
                else -> ROWS_PER_FLOOR_ONE_BLOCK * 2 + seat.rowNo
            }

        // 맨 앞 열은 맨 뒤(2층 4열)보다 약 6배 자주 골라진다.
        fun weightOf(seat: SeatResponse): Double =
            1.0 / (stageDistanceOf(seat) + 4)
    }
}
