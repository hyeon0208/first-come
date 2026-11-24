package io.github.hyeon0208.firstcome.support.test

import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class DatabaseCleanerTest : BaseServiceTest() {
    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private fun count(table: String): Long =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $table", Long::class.java)!!

    init {
        given("사용자가 한 명 있을 때") {
            fixture.createUser()

            `when`("정리하면") {
                databaseCleaner.clean()

                then("테스트가 만든 데이터는 지워진다") {
                    count("users") shouldBe 0L
                }
                then("고정 데이터(공연 3개, 좌석 1,008석)와 마이그레이션 이력은 남는다") {
                    count("shows") shouldBe 3L
                    count("seats") shouldBe 1_008L
                    count("flyway_schema_history") shouldBe 1L
                }
            }
        }
    }
}
