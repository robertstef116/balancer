package com.robert.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.robert.ConfigProperties
import com.robert.Constants
import com.robert.RabbitmqService
import com.robert.api.response.ResourceLoadData
import com.robert.enums.LBAlgorithms
import com.robert.persistence.DAORepository
import com.robert.resources.performance.DeploymentPerformanceData
import com.robert.resources.performance.PerformanceData
import com.robert.scaller.DeploymentR
import com.robert.scaller.Workflow
import io.ktor.util.logging.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class WorkflowData(
    val workflow: Workflow,
) {
    val lock = ReentrantLock()
    var removed = false
}

class DeploymentsManager : KoinComponent {
    companion object {
        private val log = KtorSimpleLogger(this::class.java.name)
        private val OM = ObjectMapper()
    }

    private val healthChecker: HealthChecker by inject()
    private val deploymentService: DeploymentService by inject()
    private val storage: DAORepository by inject()
    private val rabbitMqService: RabbitmqService by inject()
    private val workflowLoadExchangeName: String by lazy {
        ConfigProperties.getString(Constants.WORKFLOW_LOAD_EXCHANGE_KEY)
    }

    private var workflowDeployments = loadDeployments()

    private val workflows = loadWorkflows()

    fun checkDeployments() {
        removeDanglingContainersData()
        for (workflowId in workflows.keys) {
            processWorkflow(workflowId)
        }
    }

    private fun loadDeployments(): MutableMap<UUID, MutableList<DeploymentR>> {
        val workflowDeployments = mutableMapOf<UUID, MutableList<DeploymentR>>()
        for (deployment in storage.getDeployments()) {
            workflowDeployments.getOrPut(deployment.workflowId) { mutableListOf() }
                .add(deployment)
        }
        return workflowDeployments
    }

    private fun loadWorkflows(): MutableMap<UUID, WorkflowData> {
        val workflows = mutableMapOf<UUID, WorkflowData>()
        for (workflow in storage.getWorkflows()) {
            workflows[workflow.id] = WorkflowData(workflow)
        }
        return workflows
    }

    private fun removeDanglingContainersData(): Boolean {
        var hasChanges = false
        for ((workerId, healthData) in healthChecker.workersHealthData) {
            for (performanceData in healthData.deploymentsPerformance) {
                if (getDeployment(performanceData.deploymentId) == null) {
                    log.warn("Remove containers not part of the known deployment {} on worker {}", performanceData.deploymentId, workerId)
                    hasChanges = deploymentService.removeDeployment(workerId, performanceData.deploymentId, performanceData.containerId) || hasChanges
                }
            }
        }
        return hasChanges
    }

    private fun processWorkflow(workflowId: UUID): Boolean {
        workflows[workflowId]?.let {
            it.lock.withLock {
                if (it.removed) {
                    return false
                }
                log.debug("Processing workflow {}", workflowId)
                val deployments = workflowDeployments.getOrPut(workflowId) { mutableListOf() }
                var hasChanges = false
                it.workflow.maxDeployments?.let { maxDeployments ->
                    val removedDeployments = ensureWorkflowMaxDeploymentLimit(deployments, maxDeployments)
                    if (removedDeployments.isNotEmpty()) {
                        hasChanges = deployments.removeAll(removedDeployments)
                    }
                }
                it.workflow.minDeployments?.let { maxDeployments ->
                    val addedDeployments = ensureWorkflowMinDeploymentLimit(it.workflow, deployments, maxDeployments)
                    if (addedDeployments.isNotEmpty()) {
                        hasChanges = deployments.addAll(addedDeployments)
                    }
                }
                log.trace("Finished processing workflow {}, changes found: {}", workflowId, hasChanges)
                return hasChanges
            }
        }
        return false
    }

    private fun ensureWorkflowMaxDeploymentLimit(workflowDeployments: List<DeploymentR>, maxDeploymentsAllowed: Int): List<DeploymentR> {
        val removedDeployments = mutableListOf<DeploymentR>()
        repeat(workflowDeployments.size - maxDeploymentsAllowed) {
            val deployment = scaleDownWorkflow(workflowDeployments)
            if (deployment != null) {
                removedDeployments.add(deployment)
            }
        }
        return removedDeployments
    }

    private fun ensureWorkflowMinDeploymentLimit(
        workflow: Workflow,
        workflowDeployments: List<DeploymentR>,
        minDeploymentsAllowed: Int
    ): List<DeploymentR> {
        val addedDeployments = mutableListOf<DeploymentR>()
        repeat(minDeploymentsAllowed - workflowDeployments.size) {
            val deployment = scaleUpWorkflow(workflow)
            if (deployment != null) {
                addedDeployments.add(deployment)
            }
        }
        return addedDeployments
    }

    private fun scaleDownWorkflow(workflowDeployments: List<DeploymentR>): DeploymentR? {
        val randIdx = kotlin.random.Random.nextInt(workflowDeployments.size)
        val deployment = workflowDeployments[randIdx]
        log.debug("Scaling down workflow {}, removing deployment {}", deployment.workflowId, deployment.id)
        if (deploymentService.removeDeployment(deployment.workerId, deployment.id, deployment.containerId)) {
            return deployment
        }
        return null
    }

    private fun scaleUpWorkflow(workflow: Workflow): DeploymentR? {
        val workerHealth = PerformanceData.pickRandomResource(healthChecker.workersHealthData.values)
        if (workerHealth == null) {
            log.warn("Unable to find an worker for scaling up the workflow {}", workflow.id)
            return null
        }
        val worker = healthChecker.getWorker(workerHealth.workerId)
        if (worker == null) {
            log.warn("Unable to find an worker with id {} for scaling up the workflow {}", workerHealth.workerId, workflow.id)
            return null
        }
        return deploymentService.createDeployment(worker, workflow)
    }

    fun getResourcesLoad(): List<ResourceLoadData> {
        val allPerformanceData = healthChecker.workersHealthData.values
            .map { it.deploymentsPerformance }
            .flatten()
        return workflows.values.mapNotNull { (workflow) ->
            getWorkflowResourcesLoad(workflow, allPerformanceData)
        }
    }

    private fun getWorkflowResourcesLoad(workflow: Workflow, allPerformanceData: List<DeploymentPerformanceData>): ResourceLoadData? {
        val now = Instant.now().toEpochMilli()
        return workflowDeployments[workflow.id]?.let { deployments ->
            val utilization = mutableListOf<Double>()
            val paths = workflow.pathsMapping.keys.associateWith { mutableListOf<Int>() }
            for (deployment in deployments) {
                allPerformanceData.find { it.deploymentId == deployment.id }?.let { performanceData ->
                    utilization.add(performanceData.getUtilization())
                    for ((path, containerPort) in workflow.pathsMapping) {
                        paths[path]!!.add(deployment.portsMapping[containerPort]!!)
                    }
                }
            }
            ResourceLoadData(
                workflow.id,
                workflow.algorithm,
                utilization,
                paths,
                now
            )
        }
    }

    fun publishResourcesLoad() {
        log.debug("publishing resources load")
        val allPerformanceData = healthChecker.workersHealthData.values
            .map { it.deploymentsPerformance }
            .flatten()
        workflows.values.forEach { (workflow) ->
            getWorkflowResourcesLoad(workflow, allPerformanceData)?.let {
                rabbitMqService.produce(workflowLoadExchangeName, OM.writeValueAsString(it))
            }
        }
    }

    private fun getDeployment(deploymentId: UUID): DeploymentR? {
        workflows.keys
        for (workflowId in workflows.keys) {
            getDeployment(workflowId, deploymentId)?.let {
                return it
            }
        }
        return null
    }

    private fun getDeployment(workflowId: UUID, deploymentId: UUID): DeploymentR? {
        return workflowDeployments[workflowId]?.first { it.id == deploymentId }
    }

    fun getWorkflows(): Collection<Workflow> {
        return workflows.values.map { it.workflow }
    }

    fun createWorkflow(workflow: Workflow) {
        log.debug("creating workflow: {}", workflow)
        storage.createWorkflow(workflow)
        workflows[workflow.id] = WorkflowData(workflow)
    }

    fun deleteWorkflow(id: UUID): Boolean {
        log.debug("removing workflow {}", id)
        workflows.remove(id)?.let {
            it.lock.withLock {
                val deployments = workflowDeployments[id] ?: listOf()
                deployments.forEach { deployment ->
                    log.debug("Scaling down workflow {}, removing deployment {}", deployment.workflowId, deployment.id)
                    deploymentService.removeDeployment(deployment.workerId, deployment.id, deployment.containerId)
                }
                workflowDeployments.remove(id)
                it.removed = true
            }
            return true
        }
        return false
    }

    fun updateWorkflow(id: UUID, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms?): Boolean {
        log.debug("updating workflow {}: minDeployments = {}, maxDeployments = {}, algorithm = {}", id, minDeployments, maxDeployments, algorithm)
        workflows[id]?.let {
            it.lock.withLock {
                if (it.removed) {
                    return false
                }
                it.workflow.minDeployments = minDeployments
                it.workflow.maxDeployments = maxDeployments
                if (algorithm != null) {
                    it.workflow.algorithm = algorithm
                }
            }
            return true
        }
        return false
    }
}
