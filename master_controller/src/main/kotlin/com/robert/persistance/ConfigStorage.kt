package com.robert.persistance

import com.robert.DBConnector
import com.robert.StorageUtils

class ConfigStorage {
    fun setConfig(key: String, value: String) {
        DBConnector.getConnection().prepareStatement("INSERT INTO config(key, value) VALUES (?, ?)")
            .use { st ->
                StorageUtils.executeUpdate(st)
            }
    }
}
