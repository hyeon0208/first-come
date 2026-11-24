package io.github.hyeon0208.firstcome.competitor.application.dto

import io.github.hyeon0208.firstcome.competitor.application.CompetitorProperties
import io.github.hyeon0208.firstcome.performance.Difficulty

// 난이도 설명. soldOutSeconds가 null이면 매진되지 않는 난이도다.
data class DifficultyResponse(
    val difficulty: Difficulty,
    val competitorCount: Int,
    val arrivalSeconds: Long,
    val soldOutSeconds: Long?,
) {
    companion object {
        fun of(difficulty: Difficulty, setting: CompetitorProperties.DifficultySetting): DifficultyResponse =
            DifficultyResponse(
                difficulty = difficulty,
                competitorCount = setting.count,
                arrivalSeconds = setting.arrivalWindow.toSeconds(),
                soldOutSeconds = setting.soldOutSeconds,
            )
    }
}
