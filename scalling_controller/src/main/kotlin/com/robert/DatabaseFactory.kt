package com.robert

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(vararg tables: Table) {
        val driverClassName = ConfigProperties.getString("storage.driverClassName")
        val jdbcURL = ConfigProperties.getString("storage.jdbcURL")
        val username = ConfigProperties.getString("storage.username")
        val password = ConfigProperties.getString("storage.password")
        val database = Database.connect(createHikariDataSource(jdbcURL, driverClassName, username, password))

        transaction(database) {
            for (table in tables) {
                SchemaUtils.create(table)
            }
        }
    }

    private fun createHikariDataSource(
        url: String, driver: String, user: String, pass: String
    ) = HikariDataSource(HikariConfig().apply {
        driverClassName = driver
        jdbcUrl = url
        username = user
        password = pass
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })

    fun <T> dbQuery(block: () -> T) = transaction {
        block()
    }
}
