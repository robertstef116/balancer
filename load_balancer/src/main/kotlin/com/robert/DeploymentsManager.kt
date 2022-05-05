package com.robert

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.properties.Delegates

private data class WorkerResource(val worker: WorkerNode, val availableCpu: Double, val availableMemory: Double)

private data class WorkersResources(
    val totalAvailableCpu: Double, val totalAvailableMemory: Double, val workersResources: List<WorkerResource>
)

class DeploymentsManager(private val resourcesManager: ResourcesManager, private val service: Service) :
    UpdateAwareManager(Constants.WORKFLOW_SERVICE_KEY) {

    companion object {
        private val log = LoggerFactory.getLogger(DeploymentsManager::class.java)
        private val context = newSingleThreadContext("DEPLOYMENTS_MANAGER_CHECK")
    }

    private val processingLock = ReentrantLock()

    private var cpuWeight by Delegates.notNull<Float>()
    private var memoryWeight by Delegates.notNull<Float>()
    private var deploymentsCheckInterval by Delegates.notNull<Long>()

    private var workflows: List<Workflow> = emptyList()
    private var deployments = resourcesManager.getDeployments()

    private fun workflowChanged() {
        val newWorkflows = resourcesManager.getWorkflows()
        workflows = newWorkflows
    }

    fun start() {
        resourcesManager.registerInitializationWaiter {
            run()
        }
    }

    fun loadConfigs() {
        log.debug("load configs")
        processingLock.withLock {
            cpuWeight = (DynamicConfigProperties.getFloatPropertyOrDefault(Constants.CPU_WEIGHT, 0.5f))
            memoryWeight = DynamicConfigProperties.getFloatPropertyOrDefault(Constants.MEMORY_WEIGHT, 0.5f)
            deploymentsCheckInterval =
                DynamicConfigProperties.getLongPropertyOrDefault(Constants.DEPLOYMENTS_CHECK_INTERVAL, 60000)
        }
    }

    private fun run() {
        log.debug("deployment manager started")
        CoroutineScope(context).launch {
            while (true) {
                try {
                    val deploymentsPerformancePerWorker = resourcesManager.healthChecks
                        .associate { it.worker to it.deploymentsPerformance }

                    val deploymentsPerformanceList = deploymentsPerformancePerWorker.values.flatten()

                    processingLock.withLock { // lock in case of limit changes
                        // remove registered containers which are not part of a deployment
                        for ((worker, deploymentPerformanceListPerWorker) in deploymentsPerformancePerWorker) {
                            for (deploymentPerformance in deploymentPerformanceListPerWorker) {
                                val deployment = deployments.find {
                                    it.id == deploymentPerformance.deploymentId
                                }
                                if (deployment == null) {
                                    service.removeDeployment(
                                        worker,
                                        deploymentPerformance.deploymentId,
                                        deploymentPerformance.containerId
                                    )
                                }
                            }
                        }

                        val addedDeployments = mutableSetOf<Deployment>()
                        val removedDeployments = mutableSetOf<Deployment>()
                        for (workflow in workflows) {
                            val workflowDeployments = deployments.filter { it.workflowId == workflow.id }
                            var currentNumberOfDeployments = workflowDeployments.size

                            // undeploy additional containers
                            if (currentNumberOfDeployments > (workflow.maxDeployments ?: Int.MAX_VALUE)) {
                                val deployment = undeployWorkflow(workflowDeployments)
                                if (deployment != null) {
                                    removedDeployments.add(deployment)
                                }
                            }

                            // redeploy containers which are not running
                            for (deployment in deployments) {
                                val deploymentPerformance = deploymentsPerformanceList.find {
                                    it.containerId == deployment.containerId
                                }

                                if (deploymentPerformance == null) {

                                   val worker = resourcesManager.workers.find { it.id == deployment.workerId }!!
                                    service.removeDeployment(worker, deployment.id, deployment.containerId)
                                    removedDeployments.add(deployment)
                                    val newDeployment = deployWorkflow(workflow)
                                    if (newDeployment != null) { // if successfully deployed
                                        addedDeployments.add(newDeployment)
                                    } else {
                                        currentNumberOfDeployments--
                                    }
                                }
                            }

                            // deploy containers up to the lower limit
                            if (currentNumberOfDeployments < (workflow.minDeployments ?: 1)) {
                                val deployment = deployWorkflow(workflow)
                                if (deployment != null) { // if successfully deployed
                                    addedDeployments.add(deployment)
                                }
                            }
                        }

                        deployments = deployments - removedDeployments + addedDeployments

                        // TO DO: apply changes instead of reload
                        if (removedDeployments.size > 0 || addedDeployments.size>0) {
                            resourcesManager.pathsMappingChanged()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(deploymentsCheckInterval)
            }
        }
    }

    private fun deployWorkflow(workflow: Workflow): Deployment? {
        val resources = createWorkersResources(resourcesManager.healthChecks)
        log.debug("deploying workflow, {} workers available", resources.workersResources.size)

        var rand = kotlin.random.Random.nextDouble()
        for (workerResource in resources.workersResources) {
            val weight = 1 - ((1 - workerResource.availableCpu / resources.totalAvailableCpu) * cpuWeight +
                    (1 - workerResource.availableMemory / resources.totalAvailableMemory) * memoryWeight)
            if (rand < weight) {
                log.debug("deploying workflow {} on worker {}", workflow, workerResource.worker)
                return service.deployWorkflow(workerResource.worker, workflow)
            }
            rand -= weight
        }

        log.error("no resources available")
        return null
    }

    private fun undeployWorkflow(workflowDeployments: List<Deployment>): Deployment? {
        val randIdx = kotlin.random.Random.nextInt(workflowDeployments.size)
        val deployment = workflowDeployments[randIdx]
        val worker = resourcesManager.workers.find { it.id == deployment.workerId }!!
        if (service.removeDeployment(worker, deployment.id, deployment.containerId)) {
            return deployment
        }
        return null
    }

    private fun createWorkersResources(healthChecks: List<HealthChecker>): WorkersResources {
        var totalAvailableCpu = 0.0
        var totalAvailableMemory = 0.0
        val resources = healthChecks.map {
            val availableCpu = it.getAverageAvailableCpu()
            val availableMemory = it.getAverageAvailableMemory()
            totalAvailableCpu += availableCpu
            totalAvailableMemory += availableMemory
            WorkerResource(it.worker, availableCpu, availableMemory)
        }

        return WorkersResources(totalAvailableCpu, totalAvailableMemory, resources)
    }

    override fun refresh() {
        workflowChanged()
    }
}
