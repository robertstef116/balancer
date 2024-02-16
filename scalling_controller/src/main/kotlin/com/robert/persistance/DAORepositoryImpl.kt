package com.robert.persistance

import com.robert.DatabaseFactory
import com.robert.enums.LBAlgorithms
import com.robert.scaller.DeploymentR
import com.robert.scaller.WorkerR
import com.robert.scaller.WorkerStatusR
import com.robert.scaller.WorkflowR
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.*

class DAORepositoryImpl : DAORepository {
    override fun getWorkers(): List<WorkerR> = DatabaseFactory.dbQuery {
        Workers.selectAll().map { row ->
            WorkerR(
                id = row[Workers.id],
                alias = row[Workers.alias],
                host = row[Workers.host],
                port = row[Workers.health],
                status = row[Workers.status],
            )
        }
    }

    override fun createWorker(worker: WorkerR): Unit = DatabaseFactory.dbQuery {
        Workers.insert {
            it[id] = worker.id
            it[alias] = worker.alias
            it[host] = worker.host
            it[health] = worker.port
            it[status] = worker.status
        }
    }

    override fun updateWorker(id: UUID, alias: String?, status: WorkerStatusR?): Boolean {
        return Workers.update({Workers.id eq id}) {
            if (alias != null)
                it[this.alias] = alias
            if (status != null)
                it[this.status] = status
        } != 0
    }

    override fun deleteWorker(id: UUID): Boolean = DatabaseFactory.dbQuery {
        Workers.deleteWhere { Workers.id eq id } != 0
    }

    override fun getWorkflows(): List<WorkflowR> = DatabaseFactory.dbQuery {
        val workflows = Workflows.selectAll().map { row ->
            WorkflowR(
                id = row[Workflows.id],
                image = row[Workflows.image],
                memoryLimit = row[Workflows.memoryLimit],
                cpuLimit = row[Workflows.cpuLimit],
                minDeployments = row[Workflows.minDeployments],
                maxDeployments = row[Workflows.maxDeployments],
                algorithm = row[Workflows.algorithm],
                mutableMapOf()
            )
        }
        val workflowsLookupMap = mutableMapOf<UUID, WorkflowR>()
        WorkflowPathsMapping.selectAll().forEach { row ->
            val workflowId = row[WorkflowPathsMapping.workflowId]
            val workflow = workflowsLookupMap.getOrElse(workflowId) {
                workflows.find { it.id == workflowId }
            }
            if (workflow != null) {
                workflow.pathsMapping[row[WorkflowPathsMapping.path]] = row[WorkflowPathsMapping.containerPort]
            }
        }
        workflows
    }

    override fun createWorkflow(workflow: WorkflowR) = DatabaseFactory.dbQuery {
        Workflows.insert {
            it[id] = workflow.id
            it[image] = workflow.image
            it[memoryLimit] = workflow.memoryLimit
            it[cpuLimit] = workflow.cpuLimit
            it[minDeployments] = workflow.minDeployments
            it[maxDeployments] = workflow.maxDeployments
            it[algorithm] = workflow.algorithm
        }
        for ((p, cp) in workflow.pathsMapping) {
            WorkflowPathsMapping.insert {
                it[workflowId] = workflow.id
                it[path] = p
                it[containerPort] = cp
            }
        }
    }

    override fun updateWorkflow(id: UUID, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms?): Boolean = DatabaseFactory.dbQuery {
        Workflows.update({ Workflows.id eq id }) {
            it[this.minDeployments] = minDeployments
            it[this.maxDeployments] = maxDeployments
            if (algorithm != null)
                it[this.algorithm] = algorithm
        } != 0
    }

    override fun deleteWorkflow(id: UUID): Boolean = DatabaseFactory.dbQuery {
        Workflows.deleteWhere { Workflows.id eq id } != 0
    }

    override fun getDeployments(): List<DeploymentR> = DatabaseFactory.dbQuery {
        val deployments = Deployments.selectAll().map { row ->
            DeploymentR(
                id = row[Deployments.id],
                workerId = row[Deployments.workerId],
                workflowId = row[Deployments.workflowId],
                containerId = row[Deployments.containerId],
                mutableMapOf()
            )
        }
        val deploymentsLookupMap = mutableMapOf<UUID, DeploymentR>()
        DeploymentPortsMapping.selectAll().forEach { row ->
            val deploymentId = row[DeploymentPortsMapping.deploymentId]
            val deployment = deploymentsLookupMap.getOrElse(deploymentId) {
                deployments.find { it.id == deploymentId }
            }
            if (deployment != null) {
                deployment.portsMapping[row[DeploymentPortsMapping.workerPort]] = row[DeploymentPortsMapping.containerPort]
            }
        }
        deployments
    }

    override fun createDeployment(deployment: DeploymentR) = DatabaseFactory.dbQuery {
        Deployments.insert {
            it[id] = deployment.id
            it[workerId] = deployment.workerId
            it[workflowId] = deployment.workflowId
            it[containerId] = deployment.containerId
        }
        for ((wp, cp) in deployment.portsMapping) {
            DeploymentPortsMapping.insert {
                it[deploymentId] = deployment.id
                it[workerPort] = wp
                it[containerPort] = cp
            }
        }
    }

    override fun deleteDeployment(id: UUID): Boolean = DatabaseFactory.dbQuery {
        Deployments.deleteWhere { Deployments.id eq id } != 0
    }
}
