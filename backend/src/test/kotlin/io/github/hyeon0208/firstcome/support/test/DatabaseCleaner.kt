package io.github.hyeon0208.firstcome.support.test

import java.sql.Connection
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

// 테스트마다 바뀌는 데이터만 지운다. 공연과 좌석 같은 고정 데이터와 마이그레이션 이력은 남긴다.
@Component
class DatabaseCleaner(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun clean() {
        jdbcTemplate.execute(
            ConnectionCallback { connection ->
                val tableNames = tableNames(connection) - KEPT_TABLES
                connection.createStatement().use { statement ->
                    statement.execute("SET FOREIGN_KEY_CHECKS = 0")
                    try {
                        tableNames.forEach { statement.execute("DELETE FROM $it") }
                    } finally {
                        statement.execute("SET FOREIGN_KEY_CHECKS = 1")
                    }
                }
            },
        )
    }

    private fun tableNames(connection: Connection): List<String> =
        connection.metaData
            .getTables(connection.catalog, connection.schema, null, arrayOf("TABLE"))
            .use { resultSet ->
                generateSequence { if (resultSet.next()) resultSet.getString("TABLE_NAME") else null }.toList()
            }

    companion object {
        private val KEPT_TABLES = setOf("flyway_schema_history", "venues", "seats", "shows", "show_seat_prices")
    }
}
