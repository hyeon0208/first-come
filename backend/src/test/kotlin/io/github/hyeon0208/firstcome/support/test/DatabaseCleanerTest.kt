package io.github.hyeon0208.firstcome.support.test

import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class DatabaseCleanerTest : BaseServiceTest() {
    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        given("테이블에 데이터가 있을 때") {
            jdbcTemplate.update("INSERT INTO users (name) VALUES ('홍길동')")
            `when`("정리하면") {
                databaseCleaner.clean()
                then("모든 테이블이 빈다") {
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long::class.java) shouldBe 0L
                }
            }
        }
    }
}
