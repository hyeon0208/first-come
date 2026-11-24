package io.github.hyeon0208.firstcome.competitor.application

import io.github.hyeon0208.firstcome.performance.Difficulty
import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

// 가상 경쟁자 설정. 뜻과 값은 application.yml의 competitor 항목에 있다.
@ConfigurationProperties("competitor")
data class CompetitorProperties(
    val enabled: Boolean,
    val pressIntervalMin: Duration,
    val pressIntervalMax: Duration,
    val checkoutDelayMin: Duration,
    val checkoutDelayMax: Duration,
    val difficulties: Map<Difficulty, DifficultySetting>,
) {
    data class DifficultySetting(
        val count: Int,
        val arrivalWindow: Duration,
        val soldOutSeconds: Long?,
    )

    fun settingOf(difficulty: Difficulty): DifficultySetting =
        difficulties.getValue(difficulty)
}
