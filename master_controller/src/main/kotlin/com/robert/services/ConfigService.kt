package com.robert.services

import com.robert.Constants
import com.robert.DBConnector
import com.robert.StorageUtils
import com.robert.UpdateAwareService
import com.robert.persistance.ConfigStorage

class ConfigService : UpdateAwareService(Constants.CONFIG_SERVICE_KEY) {
    private val configStorage = ConfigStorage()

    fun getConfigs(): Map<String, String> {
        return configStorage.getConfigs()
    }

    fun setConfig(configs: Map<String, String>) {
        for ((key, value) in configs.entries) {
            configStorage.setConfig(key, value)
        }
        markChange()
    }
}
