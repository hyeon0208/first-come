package io.github.hyeon0208.firstcome.support.web

// 예매 화면의 모든 요청에 붙는 회차 번호 헤더. 다시 시작한 뒤 예전 화면이 보낸 요청이 새 회차에 닿지 않게 한다.
object PracticeRunHeader {
    const val NAME = "X-Practice-Run"
}
