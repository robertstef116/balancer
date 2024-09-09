package com.robert.storage

import com.robert.Env
import com.robert.storage.entities.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseInitializer {
    private val tables = listOf(Users, Workers, Workflows, WorkflowMappings, LoadBalancerAnalytics, ScalingAnalytics)

    fun initialize() {
        val driverClassName = Env.get("DB_DRIVER", "org.postgresql.Driver")
        val host = Env.get("DB_HOST")
        val port = Env.get("DB_PORT")
        val dbName = Env.get("DB_NAME")
        val protocol = Env.get("DB_PROTOCOL", "jdbc:postgresql")
        val jdbcURL = "$protocol://$host:$port/$dbName"
        val username = Env.get("DB_USER")
        val password = Env.getSecret("DB_PASSWORD")
        val database = Database.connect(HikariDataSource(HikariConfig().apply {
            this.driverClassName = driverClassName
            this.jdbcUrl = jdbcURL
            this.username = username
            this.password = password
            maximumPoolSize = 30
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