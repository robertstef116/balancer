package com.robert.storage.entities

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object ScalingAnalytics : Table() {
    val workflowId = reference("workflow_id", Workflows.id, onDelete = ReferenceOption.CASCADE)
    val replicas = integer("replicas")
    val timestamp = long("timestamp_ms")
}