package com.robert.persistance

import com.robert.DBConnector
import com.robert.StorageUtils

class ConfigStorage {
    fun getConfigs(): Map<String, String> {
        val query = "SELECT key, value FROM config"
        val configs = HashMap<String, String>()

        DBConnector.getConnection().createStatement()
            .use { st ->
                st.executeQuery(query)
                    .use { rs ->
                        while (rs.next()) {
                            configs[rs.getString("key")] = rs.getString("value")
                        }
                    }
            }

        return configs
    }

    fun setConfig(key: String, value: String) {
        DBConnector.getConnection().prepareStatement("UPDATE config SET value = ? WHERE key = ?")
            .use { st ->
                st.setString(2, key)
                st.setString(1, value)
                StorageUtils.executeUpdate(st)
            }
    }
}
