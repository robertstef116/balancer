package com.robert.persistance

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Deployments: Table() {
    val id = uuid("id").autoGenerate()
    val workerId = reference("worker_id", Workers.id, onDelete = ReferenceOption.CASCADE)
    val workflowId = reference("workflow_id", Workflows.id, onDelete = ReferenceOption.CASCADE)
    val containerId = varchar("container_id", 64)

    override val primaryKey = PrimaryKey(id)
}

object DeploymentPortsMapping: Table() {
    val deploymentId = reference("deployment_id", Deployments.id, onDelete = ReferenceOption.CASCADE)
    val workerPort = integer("worker_port")
    val containerPort = integer("container_port")
}
