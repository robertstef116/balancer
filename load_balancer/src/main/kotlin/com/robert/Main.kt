package com.robert

fun main() {
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
