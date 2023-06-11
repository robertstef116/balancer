package com.robert.persistance

import com.robert.enums.LBAlgorithms
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table


object Workflows: Table() {
    val id = uuid("id").autoGenerate()
    val image = varchar("image", 100)
    val memoryLimit = long("memory_limit").nullable()
    val cpuLimit = long("cpu_limit").nullable()
    val minDeployments = integer("min_deployments").nullable()
    val maxDeployments = integer("max_deployments").nullable()
    val algorithm = enumerationByName<LBAlgorithms>("algorithm", 25)

    override val primaryKey = PrimaryKey(id)
}

object WorkflowPathsMapping: Table() {
    val workflowId = reference("workflow_id", Workflows.id, onDelete = ReferenceOption.CASCADE)
    val path = varchar("path", 50)
    val containerPort = integer("container_port")

    override val primaryKey = PrimaryKey(path)
}
