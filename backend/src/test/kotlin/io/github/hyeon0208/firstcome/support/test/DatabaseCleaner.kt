package io.github.hyeon0208.firstcome.support.test

import java.sql.Connection
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class DatabaseCleaner(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun clean() {
        jdbcTemplate.execute(
            ConnectionCallback { connection ->
                val tableNames = tableNames(connection)
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
                generateSequence { if (resultSet.next()) resultSet.getString("TABLE_NAME") else null }
                    .filter { it != "flyway_schema_history" }
                    .toList()
            }
}
