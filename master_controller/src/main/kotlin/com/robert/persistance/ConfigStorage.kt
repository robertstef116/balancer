package com.robert.persistance

import com.robert.DBConnector
import com.robert.StorageUtils
import org.slf4j.LoggerFactory

class ConfigStorage {
    companion object {
        private val log = LoggerFactory.getLogger(ConfigStorage::class.java)
    }

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

    fun setConfig(configs: Map<String, String>) {
        DBConnector.getTransactionConnection().use { conn ->
            for ((key, value) in configs.entries) {
                conn.prepareStatement("INSERT INTO config(key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = ?")
                    .use { st ->
                        st.setString(1, key)
                        st.setString(2, value)
                        st.setString(3, value)
                        try {
                            StorageUtils.executeUpdate(st)
                        } catch (e: Exception) {
                            log.error(e.message)
                            conn.rollback()
                            throw e
                        }
                        log.debug("updated {} to {}", key, value)
                    }
            }

            conn.commit()
        }
    }
}
