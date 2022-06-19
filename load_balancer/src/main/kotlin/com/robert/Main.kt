package com.robert

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory

fun main() {
    val loggerContext: LoggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    rootLogger.level = Level.TRACE

    val storage = Storage()
    val service = Service(storage)
    service.syncWorkers()

    val resourcesManager = ResourcesManager(storage)
    val deploymentsManager = DeploymentsManager(resourcesManager, service)
    val loadBalancer = LoadBalancer(resourcesManager, service)
    val dynamicConfigsManager = DynamicConfigsManager(storage, deploymentsManager, resourcesManager, loadBalancer)
    val masterChangesManager = MasterChangesManager(service, listOf(dynamicConfigsManager, resourcesManager, deploymentsManager))
    dynamicConfigsManager.setMasterChangesManager(masterChangesManager)
    deploymentsManager.start()
    masterChangesManager.start()
    loadBalancer.start()
}
