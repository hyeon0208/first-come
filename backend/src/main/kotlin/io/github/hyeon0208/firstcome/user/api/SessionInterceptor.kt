package io.github.hyeon0208.firstcome.user.api

import io.github.hyeon0208.firstcome.support.web.CurrentUserId
import io.github.hyeon0208.firstcome.user.application.UserReadService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.util.WebUtils

// 세션 쿠키로 사용자를 찾아 요청에 남긴다. 사용자가 필요한지는 컨트롤러 인자(@CurrentUserId)가 정한다.
@Component
class SessionInterceptor(
    private val userReadService: UserReadService,
) : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        WebUtils.getCookie(request, SessionCookie.NAME)?.value
            ?.let(userReadService::findUserIdBySessionToken)
            ?.let { request.setAttribute(CurrentUserId.REQUEST_ATTRIBUTE, it) }
        return true
    }
}
