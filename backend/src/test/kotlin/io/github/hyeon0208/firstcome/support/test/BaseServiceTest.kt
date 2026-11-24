package io.github.hyeon0208.firstcome.support.test

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

// 잠금·격리 수준·제약은 DB 벤더에 따라 달라지므로 모든 테스트를 실제 MySQL(Testcontainers)에서 돌린다.
@SpringBootTest
@ActiveProfiles("test")
@Import(TestBeanConfiguration::class, MySqlContainerConfiguration::class)
abstract class BaseServiceTest : BaseSpringTest()
