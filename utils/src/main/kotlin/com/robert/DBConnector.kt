package com.robert

import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

object DBConnector {
    private val log = LoggerFactory.getLogger(DBConnector::class.java)

    private val dbUrl = ConfigProperties.getString("postgres.url")
    private val dbUsername = ConfigProperties.getString("postgres.username")
    private val dbPassword = ConfigProperties.getString("postgres.password")

    private var connection: Connection

    init {
        connection = connect()
    }

    private fun connect(): Connection {
        val props = Properties()
        props.setProperty("user", dbUsername)
        props.setProperty("password", dbPassword)
        val connection = DriverManager.getConnection(dbUrl, props)
        log.debug("connected to database successfully")
        return connection
    }

    fun getConnection(): Connection {
        if (connection.isClosed) {
            connect()
        }
        return connection
    }

    fun getTransactionConnection(): Connection {
        val connection = connect()
        connection.autoCommit = false
        return connection
    }
}
