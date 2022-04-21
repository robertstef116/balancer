package com.robert

import com.robert.exceptions.NotFoundException
import java.sql.PreparedStatement

object Storage {
    fun executeUpdate(st: PreparedStatement) {
        val res = st.executeUpdate()

        if (res < 1) {
            throw NotFoundException()
        }
    }
}
