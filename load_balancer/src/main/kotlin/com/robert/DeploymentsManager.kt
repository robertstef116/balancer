package com.robert

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.properties.Delegates

private data class WorkerResource(val worker: WorkerNode, val availableCpu: Double, val availableMemory: Double)

private data class WorkersResources(
    val totalAvailableCpu: Double, val totalAvailableMemory: Double, val workersResources: List<WorkerResource>
)

class WaitResourceInfoUpdate : RuntimeException()

class DeploymentsManager(private val resourcesManager: ResourcesManager, private val service: Service) :
    UpdateAwareManager(Constants.WORKFLOW_SERVICE_KEY), BackgroundService {

    companion object {
        private val log = LoggerFactory.getLogger(DeploymentsManager::class.java)
        private val context = Dispatchers.IO.limitedParallelism(1)
    }

    private val weightsLock = ReentrantLock()
    private var lastDeploymentsChange = Instant.now().epochSecond

    private var cpuWeight by Delegates.notNull<Float>()
    private var memoryWeight by Delegates.notNull<Float>()
    private var deploymentsCheckInterval by Delegates.notNull<Long>()

    private var workflows = emptyList<Workflow>()
    private var deployments = resourcesManager.getDeployments()

    private fun workflowChanged() {
        workflows = resourcesManager.getWorkflows()
    }

    override fun start() {
        resourcesManager.registerInitializationWaiter {
            run()
        }
    }

    fun reloadDynamicConfigs() {
        log.debug("load configs")
        weightsLock.withLock {
            cpuWeight = (DynamicConfigProperties.getFloatPropertyOrDefault(Constants.CPU_WEIGHT, 0.5f))
            memoryWeight = DynamicConfigProperties.getFloatPropertyOrDefault(Constants.MEMORY_WEIGHT, 0.5f)
        }
        deploymentsCheckInterval = DynamicConfigProperties.getLongPropertyOrDefault(Constants.DEPLOYMENTS_CHECK_INTERVAL, 60000)
    }

    private fun run() {
        log.debug("deployment manager started")
        CoroutineScope(context).launch {
            while (true) {
                log.debug("processing deployments")
                try {
                    val healthChecks = resourcesManager.healthChecks

                    for (healthCheck in healthChecks) {
                        if (healthCheck.lastCheckTimestamp < lastDeploymentsChange) {
                            throw WaitResourceInfoUpdate()
                        }
                    }

                    val deploymentsPerformancePerWorker = healthChecks
                        .associate { it.worker to it.deploymentsPerformance }

                    val deploymentsPerformanceList = deploymentsPerformancePerWorker.values.flatten()

                    // remove registered containers which are not part of a deployment
                    for ((worker, deploymentPerformanceListPerWorker) in deploymentsPerformancePerWorker) {
                        for (deploymentPerformance in deploymentPerformanceListPerWorker) {
                            val deployment = deployments.find {
                                it.id == deploymentPerformance.deploymentId
                            }
                            if (deployment == null) {
                                log.debug(
                                    "remove registered containers which are not part of a registered deployment, deploymentId={}",
                                    deploymentPerformance.deploymentId
                                )
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

                        // remove additional containers
                        if (currentNumberOfDeployments > (workflow.maxDeployments ?: Int.MAX_VALUE)) {
                            log.debug("remove additional deployments, workflowId={}", workflow.id)
                            val deployment = undeployWorkflow(workflowDeployments)
                            if (deployment != null) {
                                removedDeployments.add(deployment)
                                currentNumberOfDeployments--
                            }
                        }

                        // remove containers which are not running
                        for (deployment in workflowDeployments) {
                            val deploymentPerformance = deploymentsPerformanceList.find {
                                it.containerId == deployment.containerId
                            }

                            if (deploymentPerformance == null) {
                                val worker = resourcesManager.workers.find { it.id == deployment.workerId }
                                log.debug("remove unhealthy deployment {}", deployment.id)
                                if (worker != null) {
                                    service.removeDeployment(worker, deployment.id, deployment.containerId)
                                }
                                removedDeployments.add(deployment)
//                                val newDeployment = deployWorkflow(workflow)
//                                if (newDeployment != null) { // if successfully deployed
//                                    addedDeployments.add(newDeployment)
//                                } else {
                                currentNumberOfDeployments--
//                                }
                            }
                        }

                        // deploy containers up to the lower limit
                        if (currentNumberOfDeployments < (workflow.minDeployments ?: 1)) {
                            log.debug("less deployments than expected, {} < {}", currentNumberOfDeployments, workflow.minDeployments ?: 1)
                            val deployment = deployWorkflow(workflow)
                            if (deployment != null) { // if successfully deployed
                                log.debug("add deployments up to the lower limit, deploymentId={}", deployment.id)
                                addedDeployments.add(deployment)
                            }
                        }
                    }

                    deployments = deployments - removedDeployments + addedDeployments

                    // TODO: apply changes instead of reload
                    if (removedDeployments.size > 0 || addedDeployments.size > 0) {
                        log.debug("deployments changed, reload resources")
                        resourcesManager.pathsMappingChanged()
                        lastDeploymentsChange = Instant.now().epochSecond
                    }
                } catch (e: WaitResourceInfoUpdate) {
                    log.debug("skip check, no resource info date received since the last update")
                } catch (e: Exception) {
                    log.error("error processing deployments, err = {}", e.message)
                }

                log.debug("processing deployments done, next check in {} ms", deploymentsCheckInterval)
                delay(deploymentsCheckInterval)
            }
        }
    }

    private fun deployWorkflow(workflow: Workflow): Deployment? {
        val resources = createWorkersResources(resourcesManager.healthChecks)
        log.debug("deploying workflow, {} workers available", resources.workersResources.size)

        var rand = kotlin.random.Random.nextDouble()
        for (workerResource in resources.workersResources) {
            var weight = 1.0

            weightsLock.withLock {
                weight -= (1 - workerResource.availableCpu / resources.totalAvailableCpu) * cpuWeight +
                        (1 - workerResource.availableMemory / resources.totalAvailableMemory) * memoryWeight
            }

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
        log.debug("remove additional deployments, deploymentId={}", deployment.id)
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
