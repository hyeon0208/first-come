package io.github.hyeon0208.firstcome.user.api

import io.github.hyeon0208.firstcome.user.application.UserWriteService
import io.github.hyeon0208.firstcome.user.application.dto.CreateUserRequest
import io.github.hyeon0208.firstcome.user.application.dto.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userWriteService: UserWriteService,
) : UserApi {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createUser(@RequestBody request: CreateUserRequest): UserResponse =
        userWriteService.createUser(request)
}
