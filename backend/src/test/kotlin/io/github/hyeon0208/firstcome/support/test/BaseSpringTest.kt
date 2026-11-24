package io.github.hyeon0208.firstcome.support.test

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import org.springframework.beans.factory.annotation.Autowired

abstract class BaseSpringTest : BehaviorSpec() {
    @Autowired
    protected lateinit var testClock: TestClock

    @Autowired
    protected lateinit var fakePaymentClient: FakePaymentClient

    @Autowired
    protected lateinit var fixture: TestFixture

    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    init {
        isolationMode = IsolationMode.InstancePerRoot
    }

    // beforeTest는 given, when 컨테이너에도 실행되므로, given에서 준비한 상태가 지워지지 않게 루트에서만 초기화한다.
    override suspend fun beforeTest(testCase: TestCase) {
        if (testCase.parent == null) {
            testClock.reset()
            fakePaymentClient.reset()
            databaseCleaner.clean()
        }
    }
}
