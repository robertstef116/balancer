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

    private lateinit var connection: Connection

    init {
        connect()
    }

    private fun connect() {
        val props = Properties()
        props.setProperty("user", dbUsername)
        props.setProperty("password", dbPassword)
        connection = DriverManager.getConnection(dbUrl, props)
        log.debug("connected to database successfully")
    }

    fun getConnection(): Connection {
        if (connection.isClosed) {
            connect()
        }
        return connection
    }
}
