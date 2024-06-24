package com.robert.persistence

import com.robert.enums.LBAlgorithms
import com.robert.scaller.DeploymentR
import com.robert.scaller.Worker
import com.robert.scaller.WorkerState
import com.robert.scaller.Workflow
import java.util.*

interface DAORepository {
    fun getWorkers(): List<Worker>
    fun createWorker(worker: Worker)
    fun updateWorker(id: UUID, alias: String?, status: WorkerState?): Boolean
    fun deleteWorker(id: UUID): Boolean

    fun getWorkflows(): List<Workflow>
    fun createWorkflow(workflow: Workflow)
    fun updateWorkflow(id: UUID, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms?): Boolean
    fun deleteWorkflow(id: UUID): Boolean

    fun getDeployments(): List<DeploymentR>
    fun createDeployment(deployment: DeploymentR)
    fun deleteDeployment(id: UUID): Boolean
}
