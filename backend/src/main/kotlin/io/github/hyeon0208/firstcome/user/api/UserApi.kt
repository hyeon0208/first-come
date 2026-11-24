package io.github.hyeon0208.firstcome.user.api

import io.github.hyeon0208.firstcome.support.error.ErrorResponse
import io.github.hyeon0208.firstcome.user.application.dto.CreateUserRequest
import io.github.hyeon0208.firstcome.user.application.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "User", description = "사용자")
interface UserApi {
    @Operation(
        summary = "닉네임으로 시작하기",
        description = "닉네임(앞뒤 공백을 뺀 2~12자)으로 사용자를 만들고 세션 쿠키를 내려 준다. 가입은 없고, 닉네임이 같아도 따로 만들어진다.",
    )
    @ApiResponse(responseCode = "201", description = "사용자를 만들고 세션 쿠키를 내려 줌")
    @ApiResponse(
        responseCode = "400",
        description = "INVALID_REQUEST: 닉네임 길이",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun createUser(request: CreateUserRequest): ResponseEntity<UserResponse>

    @Operation(summary = "내 정보", description = "세션 쿠키의 사용자를 돌려준다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "401",
        description = "UNAUTHENTICATED: 세션 쿠키가 없거나 맞는 사용자가 없음",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getMe(userId: Long): UserResponse
}
