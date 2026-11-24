package io.github.hyeon0208.firstcome.competitor.application

import io.github.hyeon0208.firstcome.competitor.application.dto.DifficultyResponse
import io.github.hyeon0208.firstcome.performance.Difficulty
import org.springframework.stereotype.Service

@Service
class DifficultyReadService(
    private val competitorProperties: CompetitorProperties,
) {
    fun getDifficulties(): List<DifficultyResponse> =
        Difficulty.entries.map { DifficultyResponse.of(it, competitorProperties.settingOf(it)) }
}
