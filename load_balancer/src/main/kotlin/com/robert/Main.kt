package com.robert

fun main() {
    val storage = Storage()
    val service = Service(storage)
    val resourcesManager = ResourcesManager(storage)
    val deploymentsManager = DeploymentsManager(resourcesManager, service)
    deploymentsManager.start()
    val loadBalancer = LoadBalancer(resourcesManager)
}
