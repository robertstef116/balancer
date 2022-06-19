package com.robert

import com.robert.algorithms.WeightedResponseTimeAlgorithm

class DynamicConfigsManager(
    private val storage: Storage,
    private val deploymentsManager: DeploymentsManager,
    private val resourcesManager: ResourcesManager,
    private val loadBalancer: LoadBalancer
) : UpdateAwareManager(Constants.CONFIG_SERVICE_KEY) {
    private var configs: Map<String, String> = storage.getConfigs()

    init {
        DynamicConfigProperties.setPropertiesData(configs)
        deploymentsManager.reloadDynamicConfigs()
        HealthChecker.reloadDynamicConfigs()
        WeightedResponseTimeAlgorithm.reloadDynamicConfigs()
        resourcesManager.reloadDynamicConfigs()
        loadBalancer.reloadDynamicConfigs()
    }

    private lateinit var masterChangesManager: MasterChangesManager

    fun setMasterChangesManager(masterChangesManager: MasterChangesManager) {
        masterChangesManager.reloadDynamicConfigs()
        this.masterChangesManager = masterChangesManager
    }

    override fun refresh() {
        val newConfigs = storage.getConfigs()

        DynamicConfigProperties.setPropertiesData(newConfigs)

        if (configs[Constants.CPU_WEIGHT] != newConfigs[Constants.CPU_WEIGHT] ||
            configs[Constants.MEMORY_WEIGHT] != newConfigs[Constants.MEMORY_WEIGHT] ||
            configs[Constants.DEPLOYMENTS_CHECK_INTERVAL] != newConfigs[Constants.DEPLOYMENTS_CHECK_INTERVAL]
        ) {
            deploymentsManager.reloadDynamicConfigs()
        }

        if (configs[Constants.NUMBER_RELEVANT_PERFORMANCE_METRICS] != newConfigs[Constants.NUMBER_RELEVANT_PERFORMANCE_METRICS] ||
            configs[Constants.HEALTH_CHECK_TIMEOUT] != newConfigs[Constants.HEALTH_CHECK_TIMEOUT] ||
            configs[Constants.HEALTH_CHECK_INTERVAL] != newConfigs[Constants.HEALTH_CHECK_INTERVAL] ||
            configs[Constants.HEALTH_CHECK_MAX_FAILURES] != newConfigs[Constants.HEALTH_CHECK_MAX_FAILURES]) {
            HealthChecker.reloadDynamicConfigs()
        }

        if (configs[Constants.COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL] != newConfigs[Constants.COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL]) {
            WeightedResponseTimeAlgorithm.reloadDynamicConfigs()
        }

        if (configs[Constants.WORKERS_CHECK_INTERVAL] != newConfigs[Constants.WORKERS_CHECK_INTERVAL]) {
            resourcesManager.reloadDynamicConfigs()
        }

        if (configs[Constants.MASTER_CHANGES_CHECK_INTERVAL] != newConfigs[Constants.MASTER_CHANGES_CHECK_INTERVAL]) {
            masterChangesManager.reloadDynamicConfigs()
        }

        if (configs[Constants.PROCESSING_SOCKET_BUFFER_LENGTH] != newConfigs[Constants.PROCESSING_SOCKET_BUFFER_LENGTH]) {
            loadBalancer.reloadDynamicConfigs()
        }

        configs = newConfigs
    }
}
