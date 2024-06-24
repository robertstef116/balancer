package com.robert.storage.entities

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object WorkflowMappings : Table() {
    val workflowId = reference("workflow_id", Workflows.id, onDelete = ReferenceOption.CASCADE)
    val port = integer("port")
    val path = varchar("path", 35)

    override val primaryKey = PrimaryKey(path)
}