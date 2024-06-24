package com.robert.storage.repository.exposed

import com.robert.enums.LBAlgorithms
import com.robert.scaller.Workflow
import com.robert.storage.entities.WorkflowMappings
import com.robert.storage.entities.Workflows
import com.robert.storage.repository.WorkflowRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

class WorkflowRepositoryImpl : WorkflowRepository {
    override fun getAll(): Collection<Workflow> = transaction {
        val workflowsMapping = mutableMapOf<UUID, MutableMap<String, Int>>()
        val workflows = mutableListOf<Workflow>()

        (Workflows innerJoin WorkflowMappings)
            .selectAll()
            .map { row ->
                val id = row[Workflows.id]
                workflowsMapping.getOrPut(id) {
                    val pathMapping = mutableMapOf<String, Int>()
                    workflows.add(
                        Workflow(
                            id = id,
                            image = row[Workflows.image],
                            memoryLimit = row[Workflows.memoryLimit],
                            cpuLimit = row[Workflows.cpuLimit],
                            minDeployments = row[Workflows.minDeployments],
                            maxDeployments = row[Workflows.maxDeployments],
                            algorithm = row[Workflows.algorithm],
                            pathMapping
                        )
                    )
                    pathMapping
                }.let {
                    it[row[WorkflowMappings.path]] = row[WorkflowMappings.port]
                }
            }

        workflows
    }

    override fun create(workflow: Workflow): Unit = transaction {
        Workflows.insert {
            it[id] = workflow.id
            it[image] = workflow.image
            it[memoryLimit] = workflow.memoryLimit
            it[cpuLimit] = workflow.cpuLimit
            workflow.minDeployments?.let { min ->
                it[minDeployments] = min
            }
            workflow.maxDeployments?.let { max ->
                it[maxDeployments] = max
            }
            it[algorithm] = workflow.algorithm
        }
        workflow.pathsMapping.entries.forEach { pathMapping ->
            WorkflowMappings.insert {
                it[workflowId] = workflow.id
                it[path] = pathMapping.key
                it[port] = pathMapping.value
            }
        }
    }

    override fun find(id: UUID): Workflow? = transaction {
        var workflow: Workflow? = null
        val pathMapping = mutableMapOf<String, Int>()
        Workflows.selectAll()
            .where { Workflows.id eq id }
            .map { row ->
                if (workflow == null) {
                    workflow = Workflow(
                        id = id,
                        image = row[Workflows.image],
                        memoryLimit = row[Workflows.memoryLimit],
                        cpuLimit = row[Workflows.cpuLimit],
                        minDeployments = row[Workflows.minDeployments],
                        maxDeployments = row[Workflows.maxDeployments],
                        algorithm = row[Workflows.algorithm],
                        pathMapping
                    )
                }
                pathMapping[row[WorkflowMappings.path]] = row[WorkflowMappings.port]
            }

        workflow
    }

    override fun delete(id: UUID): Boolean = transaction {
        WorkflowMappings.deleteWhere { workflowId eq id }
        Workflows.deleteWhere { Workflows.id eq id } != 0
    }

    override fun update(id: UUID, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms?): Boolean = transaction {
        Workflows.update({ Workflows.id eq id }) {
            if (minDeployments != null)
                it[this.minDeployments] = minDeployments
            if (maxDeployments != null)
                it[this.maxDeployments] = maxDeployments
            if (algorithm != null)
                it[this.algorithm] = algorithm
        } != 0
    }
}