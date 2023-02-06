package com.robert

import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ServerException
import org.slf4j.LoggerFactory
import java.sql.PreparedStatement

object StorageUtils {
    private val log = LoggerFactory.getLogger(this::class.java)
    fun executeUpdate(st: PreparedStatement) {
        val res = st.executeUpdate()

        log.debug("execute update, {} rows affected", res)

        if (res < 1) {
            throw NotFoundException()
        }
    }

    fun executeInsert(st: PreparedStatement) {
        val res = st.executeUpdate()

        log.debug("execute insert, {} rows added", res)

        if (res < 1) {
            throw ServerException()
        }
    }

    fun executeInsert(query: String) {
        DBConnector.getConnection().createStatement().use { st ->
            val res = st.executeUpdate(query)

            log.debug("execute insert, {} rows added", res)

            if (res < 1) {
                throw ServerException()
            }
        }
    }
}
