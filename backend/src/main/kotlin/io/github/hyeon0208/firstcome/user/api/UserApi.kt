package io.github.hyeon0208.firstcome.user.api

import io.github.hyeon0208.firstcome.support.error.ErrorResponse
import io.github.hyeon0208.firstcome.user.application.dto.CreateUserRequest
import io.github.hyeon0208.firstcome.user.application.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User", description = "사용자")
interface UserApi {
    @Operation(
        summary = "사용자 등록",
        description = "이름으로 사용자를 등록한다. 이름은 앞뒤 공백을 뺀 1~50자다. 이름이 같아도 따로 등록된다.",
    )
    @ApiResponse(responseCode = "201", description = "등록 성공")
    @ApiResponse(
        responseCode = "400",
        description = "INVALID_REQUEST: 이름 길이",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun createUser(request: CreateUserRequest): UserResponse
}
