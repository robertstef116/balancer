package com.robert

import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ServerException
import java.sql.PreparedStatement

object StorageUtils {
    fun executeUpdate(st: PreparedStatement) {
        val res = st.executeUpdate()

        if (res < 1) {
            throw NotFoundException()
        }
    }

    fun executeInsert(st: PreparedStatement) {
        val res = st.executeUpdate()

        if (res < 1) {
            throw ServerException()
        }
    }
}
