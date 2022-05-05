package com.robert

class DynamicConfigsManager(
    private val storage: Storage,
    private val deploymentsManager: DeploymentsManager,
    private val resourcesManager: ResourcesManager,
    private val loadBalancer: LoadBalancer
) :
    UpdateAwareManager(Constants.CONFIG_SERVICE_KEY) {
    private val configs: Map<String, String>

    init {
        configs = storage.getConfigs()
        DynamicConfigProperties.setPropertiesData(configs)
        deploymentsManager.loadConfigs()
        resourcesManager.loadConfigs()
        loadBalancer.loadConfigs()
    }

    private lateinit var masterChangesManager: MasterChangesManager

    fun setMasterChangesManager(masterChangesManager: MasterChangesManager) {
        masterChangesManager.loadConfigs()
        this.masterChangesManager = masterChangesManager
    }

    override fun refresh() {
        val newConfigs = storage.getConfigs()

        DynamicConfigProperties.setPropertiesData(configs)

        if (configs[Constants.CPU_WEIGHT] != newConfigs[Constants.CPU_WEIGHT] ||
            configs[Constants.MEMORY_WEIGHT] != newConfigs[Constants.MEMORY_WEIGHT] ||
            configs[Constants.DEPLOYMENTS_CHECK_INTERVAL] != newConfigs[Constants.DEPLOYMENTS_CHECK_INTERVAL]
        ) {
            deploymentsManager.loadConfigs()
        }

        if (configs[Constants.NUMBER_RELEVANT_PERFORMANCE_METRICS] != newConfigs[Constants.NUMBER_RELEVANT_PERFORMANCE_METRICS] ||
            configs[Constants.HEALTH_CHECK_TIMEOUT] != newConfigs[Constants.HEALTH_CHECK_TIMEOUT] ||
            configs[Constants.HEALTH_CHECK_INTERVAL] != newConfigs[Constants.HEALTH_CHECK_INTERVAL] ||
            configs[Constants.HEALTH_CHECK_MAX_FAILURES] != newConfigs[Constants.HEALTH_CHECK_MAX_FAILURES]
        ) {
            resourcesManager.loadConfigs()
        }

        if (configs[Constants.MASTER_CHANGES_CHECK_INTERVAL] != newConfigs[Constants.MASTER_CHANGES_CHECK_INTERVAL]) {
            masterChangesManager.loadConfigs()
        }

        if (configs[Constants.PROCESSING_SOCKET_BUFFER_LENGTH] != newConfigs[Constants.PROCESSING_SOCKET_BUFFER_LENGTH]) {
            loadBalancer.loadConfigs()
        }
    }
}
