package com.robert.storage

import com.robert.Env
import com.robert.storage.entities.Users
import com.robert.storage.entities.Workers
import com.robert.storage.entities.WorkflowMappings
import com.robert.storage.entities.Workflows
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseInitializer {
    private val tables = listOf(Users, Workers, Workflows, WorkflowMappings)

    fun initialize() {
        val driverClassName = Env.get("DB_DRIVER", "org.postgresql.Driver")
        val host = Env.get("DB_HOST")
        val port = Env.get("DB_PORT")
        val dbName = Env.get("DB_NAME")
        val protocol = Env.get("DB_PROTOCOL", "jdbc:postgresql")
        val jdbcURL = "$protocol://$host:$port/$dbName"
        val username = Env.get("DB_USER")
        val password = Env.get("DB_PASSWORD")
        val database = Database.connect(HikariDataSource(HikariConfig().apply {
            this.driverClassName = driverClassName
            this.jdbcUrl = jdbcURL
            this.username = username
            this.password = password
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }))

        transaction(database) {
            for (table in tables) {
                SchemaUtils.create(table)
            }
        }
    }
}