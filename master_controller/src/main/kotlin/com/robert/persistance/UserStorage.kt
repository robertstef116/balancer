package com.robert.persistance

import com.robert.DBConnector

class UserStorage {
    fun findUser(username: String, password: String): String? {
        DBConnector.getConnection().prepareStatement("SELECT username FROM users where username = ? and password = ?")
            .use { st ->
                st.setString(1, username)
                st.setString(2, password)
                st.executeQuery()
                    .use { rs ->
                        if (rs.next()) {
                            return rs.getString("username")
                        }
                    }
            }

        return null
    }
}
