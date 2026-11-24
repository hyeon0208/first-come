package io.github.hyeon0208.firstcome.performance.api

import io.github.hyeon0208.firstcome.performance.application.PerformanceWriteService
import io.github.hyeon0208.firstcome.performance.application.dto.CreatePerformanceRequest
import io.github.hyeon0208.firstcome.performance.application.dto.PerformanceResponse
import io.github.hyeon0208.firstcome.support.web.CurrentUserId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/performances")
class PerformanceController(
    private val performanceWriteService: PerformanceWriteService,
) : PerformanceApi {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createPerformance(@CurrentUserId userId: Long, @RequestBody request: CreatePerformanceRequest): PerformanceResponse =
        performanceWriteService.createPerformance(userId, request)

    @GetMapping("/current")
    override fun getCurrentPerformance(@CurrentUserId userId: Long): ResponseEntity<PerformanceResponse> =
        performanceWriteService.findCurrentPerformance(userId)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()

    @PostMapping("/current/abandon")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun abandonCurrentPerformance(@CurrentUserId userId: Long) {
        performanceWriteService.abandon(userId)
    }
}
