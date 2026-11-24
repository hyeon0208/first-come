package io.github.hyeon0208.firstcome.paymentmock

import java.time.Duration
import org.springframework.stereotype.Component

// 지금 주입된 장애. 주입 API를 열지 않으면 항상 NONE이다.
@Component
class FaultSettings {
    @Volatile
    var mode: FaultMode = FaultMode.NONE

    @Volatile
    var delay: Duration = Duration.ZERO
}
