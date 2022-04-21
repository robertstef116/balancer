package com.robert.persistance

import com.robert.DBConnector

class UserStorage {
    fun findUser(username: String, password: String): String? {
        val conn = DBConnector.getConnection()
        val st = conn.prepareStatement("SELECT username FROM users where username = ? and password = ?")

        st.use {
            st.setString(1, username)
            st.setString(2, password)
            val rs = st.executeQuery()

            rs.use {
                if (rs.next()) {
                    return rs.getString("username")
                }
            }
        }

        return null
    }
}
