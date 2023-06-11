package com.robert.persistance

import com.robert.scaller.DeploymentR
import com.robert.scaller.WorkerR
import com.robert.scaller.WorkflowR
import java.util.*

interface DAORepository {
    fun getWorkers(): List<WorkerR>
    fun createWorker(worker: WorkerR)
    fun deleteWorker(id: UUID): Boolean

    fun getWorkflows(): List<WorkflowR>
    fun createWorkflow(workflow: WorkflowR)
    fun deleteWorkflow(id: UUID): Boolean

    fun getDeployments(): List<DeploymentR>
    fun createDeployment(deployment: DeploymentR)
    fun deleteDeployment(id: UUID): Boolean
}
