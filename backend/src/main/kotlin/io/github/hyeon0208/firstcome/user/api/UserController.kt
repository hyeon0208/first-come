package io.github.hyeon0208.firstcome.user.api

import io.github.hyeon0208.firstcome.support.web.CurrentUserId
import io.github.hyeon0208.firstcome.user.application.UserReadService
import io.github.hyeon0208.firstcome.user.application.UserWriteService
import io.github.hyeon0208.firstcome.user.application.dto.CreateUserRequest
import io.github.hyeon0208.firstcome.user.application.dto.UserResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userWriteService: UserWriteService,
    private val userReadService: UserReadService,
    private val sessionCookie: SessionCookie,
) : UserApi {
    @PostMapping
    override fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        val session = userWriteService.createUser(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.SET_COOKIE, sessionCookie.of(session.sessionToken).toString())
            .body(session.user)
    }

    @GetMapping("/me")
    override fun getMe(@CurrentUserId userId: Long): UserResponse =
        userReadService.getUser(userId)
}
