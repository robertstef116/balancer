package com.robert

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates

class MasterChangesManager(

    private val service: Service,
    private val managers: List<UpdateAwareManager>
) {
    companion object {
        private val log = LoggerFactory.getLogger(MasterChangesManager::class.java)
        private val context = newSingleThreadContext("DEPLOYMENTS_MANAGER_CHECK")
        private val MASTER_HOST = ConfigProperties.getString("master_controller.host")!!
        private val MASTER_PORT = ConfigProperties.getInteger("master_controller.port")!!
    }

    private var masterChangesCheckInterval by Delegates.notNull<Long>()
    private var changes: Map<String, Any> = HashMap()
    private var hash = ""

    fun start() {
        run()
    }

    private fun run() {
        log.debug("master changes detector started")
        CoroutineScope(context).launch {
            while (true) {
                val newChanges = service.getMasterChanges(MASTER_HOST, MASTER_PORT)
                if (newChanges != null) {
                    val newHash = newChanges[Constants.HASH_KEY]!!
                    if (hash != newHash) {
                        log.debug("detecting a different hash, triggering all changes")
                        managers.forEach { it.refresh() }
                    } else {
                        newChanges.forEach {
                            if (it.value != changes[it.key]) {
                                val manager = managers.find { manager -> manager.key == it.key }
                                if (manager != null) {
                                    manager.refresh()
                                } else {
                                    log.warn("no manager found for key {}", it.key)
                                }
                            }
                        }
                    }
                    hash = newHash.toString()
                    changes = newChanges
                } else {
                    log.warn("skyping changes comparison")
                }
                delay(masterChangesCheckInterval)
            }
        }
    }

    fun loadConfigs() {
        log.debug("load configs")
        masterChangesCheckInterval =
            DynamicConfigProperties.getLongPropertyOrDefault(Constants.MASTER_CHANGES_CHECK_INTERVAL, 30000)
    }
}
