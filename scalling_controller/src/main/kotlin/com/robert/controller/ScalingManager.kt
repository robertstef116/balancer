package com.robert.controller

import com.robert.Constants
import com.robert.annotations.Scheduler
import com.robert.annotations.SchedulerConsumer
import com.robert.api.response.ResourceLoadData
import com.robert.persistance.DAORepository
import com.robert.scaller.WorkflowR
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.LocalDateTime

@Scheduler
class ScalingManager : KoinComponent {
    private val healthChecker: HealthChecker by inject()
    private val deploymentsManager: DeploymentsManager by inject()
    private val repository: DAORepository by inject()
    private val skipsBetweenScalingChecks = 3
    private var checkCount = 0

    @SchedulerConsumer(name = "ScalingManager", interval = "\${${Constants.HEALTH_CHECK_INTERVAL}:30s}")
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

    fun getWorkflows(): Collection<WorkflowR> {
        return deploymentsManager.getWorkflows()
    }

    fun createWorkflow(workflowR: WorkflowR) {

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
