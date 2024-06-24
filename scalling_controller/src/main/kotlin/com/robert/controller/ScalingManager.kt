package com.robert.controller

import com.robert.Constants
import com.robert.annotations.Scheduler
import com.robert.annotations.SchedulerConsumer
import com.robert.api.response.ResourceLoadData
import com.robert.enums.LBAlgorithms
import com.robert.exceptions.NotFoundException
import com.robert.persistence.DAORepository
import com.robert.scaller.Worker
import com.robert.scaller.WorkerState
import com.robert.scaller.Workflow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

@Scheduler
class ScalingManager : KoinComponent {
    private val healthChecker: HealthChecker by inject()
    private val deploymentsManager: DeploymentsManager by inject()
    private val repository: DAORepository by inject()
    private val skipsBetweenScalingChecks = 3
    private var checkCount = 0

    @SchedulerConsumer(name = "ScalingManager", interval = "\${${Constants.RESCALE_INTERVAL}:30s}")
    fun check() {
        val workerFailed = healthChecker.checkWorkers()
        if (checkCount == 0 || workerFailed) {
            deploymentsManager.checkDeployments()
        }
        deploymentsManager.publishResourcesLoad()
        checkCount = (if (workerFailed) 1 else (checkCount + 1)) % skipsBetweenScalingChecks
    }

    fun getResourcesLoad(): List<ResourceLoadData> {
        return deploymentsManager.getResourcesLoad()
    }

    fun getWorkflows(): Collection<Workflow> {
        return deploymentsManager.getWorkflows()
    }

    fun createWorkflow(image: String, memoryLimit: Long?, cpuLimit: Long?, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms, pathsMapping: MutableMap<String, Int>) {
        val workflow = Workflow(
            UUID.randomUUID(),
            image,
            memoryLimit,
            cpuLimit,
            minDeployments,
            maxDeployments,
            algorithm,
            pathsMapping,
        )
        deploymentsManager.createWorkflow(workflow)
        repository.createWorkflow(workflow)
    }

    fun updateWorkflow(id: UUID, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms?) {
        var updated = deploymentsManager.updateWorkflow(id, minDeployments, maxDeployments, algorithm)
        updated = updated || repository.updateWorkflow(id, minDeployments, maxDeployments, algorithm)
        if (!updated) {
            throw NotFoundException()
        }
    }

    fun deleteWorkflow(id: UUID) {
        var deleted = deploymentsManager.deleteWorkflow(id)
        deleted = deleted || repository.deleteWorkflow(id)
        if (!deleted) {
            throw NotFoundException()
        }
    }

    fun createWorker(alias: String, host: String, port: Int, status: WorkerState) {
        val worker = Worker(
            UUID.randomUUID(),
            alias,
            host,
            port,
            status
        )
        healthChecker.createWorker(worker)
        repository.createWorker(worker)
    }

    fun updateWorker(id: UUID, alias: String?, status: WorkerState?) {
        var updated = healthChecker.updateWorker(id, alias, status)
        updated = updated || repository.updateWorker(id, alias, status)
        if (!updated) {
            throw NotFoundException()
        }
    }

    fun deleteWorker(id: UUID) {
        var deleted = healthChecker.deleteWorker(id)
        deleted = deleted || repository.deleteWorker(id)
        if (!deleted) {
            throw NotFoundException()
        }
    }

//        val worker = WorkerR(
//            UUID.randomUUID(),
//            "w1",
//            "localhost",
//            8081,
//            WorkerStatusR.ENABLED
//        )
//        val workflow = WorkflowR(
//            UUID.randomUUID(),
//            "nginx:latest",
//            null,
//            null,
//            1,
//            3,
//            LBAlgorithms.RANDOM,
//            mutableMapOf(
//                "/test" to 331,
//                "/testme" to 332
//            )
//        )
//        val deployment = DeploymentR(
//            UUID.randomUUID(),
//            worker.id,
//            workflow.id,
//            "asd",
//            mutableMapOf(
//                331 to 325,
//                332 to 329
//            )
//        )
//        repository.createWorker(worker)
//        repository.createWorkflow(workflow)
//        repository.createDeployment(deployment)
//        println(repository.getWorkers())
//        println(repository.getWorkflows())
//        println(repository.getDeployments())
}
