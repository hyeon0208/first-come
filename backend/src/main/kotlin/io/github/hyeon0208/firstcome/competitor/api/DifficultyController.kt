package io.github.hyeon0208.firstcome.competitor.api

import io.github.hyeon0208.firstcome.competitor.application.DifficultyReadService
import io.github.hyeon0208.firstcome.competitor.application.dto.DifficultyResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/difficulties")
class DifficultyController(
    private val difficultyReadService: DifficultyReadService,
) : DifficultyApi {
    @GetMapping
    override fun getDifficulties(): List<DifficultyResponse> =
        difficultyReadService.getDifficulties()
}
