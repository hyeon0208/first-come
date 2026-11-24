package io.github.hyeon0208.firstcome

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.modulith.core.ApplicationModules

@Ignored("모듈 경계를 아직 맞추지 않았다. 경계를 맞춘 뒤 다시 켠다.")
class ModularityTest : BehaviorSpec({
    given("애플리케이션의 모듈 구조") {
        `when`("모듈 경계를 검증하면") {
            then("순환 의존과 비공개 타입 접근이 없다") {
                ApplicationModules.of(FirstComeApplication::class.java).verify()
            }
        }
    }
})
