package io.github.hyeon0208.firstcome.support.web

// 세션 쿠키로 확인한 사용자 id를 컨트롤러 인자로 받는다. 쿠키가 없거나 맞는 사용자가 없으면 401로 거절된다.
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentUserId {
    companion object {
        const val REQUEST_ATTRIBUTE = "currentUserId"
    }
}
