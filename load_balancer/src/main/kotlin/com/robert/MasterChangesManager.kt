package com.robert

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates

class MasterChangesManager(private val service: Service, private val managers: List<UpdateAwareManager>) : BackgroundService {
    companion object {
        private val log = LoggerFactory.getLogger(MasterChangesManager::class.java)
        private val context = Dispatchers.IO.limitedParallelism(1)
    }

    private var masterChangesCheckInterval by Delegates.notNull<Long>()
    private var init = false

    override fun start() {
        run()
    }

    private fun run() {
        log.debug("master changes detector started")
        if (!init) {
            managers.forEach{
                it.refresh()
            }
        }

        CoroutineScope(context).launch {
            while (true) {
                log.debug("detect master changes")
                val changes = service.getUpdatedServicesConfig()
                if (changes != null) {
                    changes.forEach {
                        val manager = managers.find { manager -> manager.key == it }
                        if (manager != null) {
                            manager.refresh()
                        } else {
                            log.warn("no manager found for key {}", it)
                        }
                    }
                } else {
                    log.warn("skipping changes comparison, unable to fetch changes")
                }
                log.debug("detect master changes done, next check in {} ms", masterChangesCheckInterval)
                delay(masterChangesCheckInterval)
            }
        }
    }

    fun reloadDynamicConfigs() {
        log.debug("load configs")
        masterChangesCheckInterval = DynamicConfigProperties.getLongPropertyOrDefault(Constants.MASTER_CHANGES_CHECK_INTERVAL, 30000)
    }
}
