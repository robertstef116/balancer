package com.robert.storage.repository

import com.robert.enums.LBAlgorithms
import com.robert.resources.Workflow
import java.util.*

interface WorkflowRepository {
    fun getAll(): Collection<Workflow>
    fun create(workflow: Workflow)
    fun find(id: UUID): Workflow?
    fun delete(id: UUID): Boolean
    fun update(id: UUID, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms?): Boolean
}