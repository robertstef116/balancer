package com.robert.controller

import com.robert.enums.LBAlgorithms
import com.robert.logger
import com.robert.scaller.Workflow
import com.robert.storage.repository.WorkflowRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class WorkflowController : KoinComponent {
    companion object {
        val LOG by logger()
    }

    private val workflowRepository: WorkflowRepository by inject()

    private val workflows = workflowRepository.getAll().toMutableList()

    init {
        LOG.info("Loaded {} workflow(s)", workflows.size)
    }

    @Synchronized
    fun getWorkflows(): List<Workflow> {
        return workflows
    }

    @Synchronized
    fun getWorkflow(id: UUID): Workflow? {
        return workflows.find { it.id == id }
    }

    @Synchronized
    fun addWorkflow(id: UUID, image: String, cpuLimit: Long, memoryLimit: Long, minDeployments: Int?, maxDeployment: Int?, algorithm: LBAlgorithms, pathsMapping: Map<String, Int>): Boolean {
        LOG.debug("Adding workflow with id {} - { image = {}, algorithm = {} }", id, image, algorithm)
        val workflow = Workflow(id, image, cpuLimit, memoryLimit, minDeployments, maxDeployment, algorithm, pathsMapping)
        return try {
            workflows.add(workflow)
            workflowRepository.create(workflow)
            true
        } catch (e: Exception) {
            LOG.error("Unable to create workflow", e)
            reload()
            false
        }
    }

    @Synchronized
    fun updateWorkflow(id: UUID, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms): Boolean {
        LOG.debug("Updating workflow with id {} - { minDeployments = {}, maxDeployment = {}, algorithm = {} }", id, minDeployments, maxDeployments, algorithm)
        workflowRepository.update(id, minDeployments, maxDeployments, algorithm)
        return try {
            val updated = workflows.find { it.id == id }
                ?.also {
                    it.minDeployments = minDeployments
                    it.maxDeployments = maxDeployments
                    it.algorithm = algorithm
                } != null
            if (updated) {
                workflowRepository.update(id, minDeployments, maxDeployments, algorithm)
            }
            updated
        } catch (e: Exception) {
            LOG.error("Unable to update workflow", e)
            reload()
            false
        }
    }

    @Synchronized
    fun removeWorkflow(id: UUID): Boolean {
        LOG.debug("Removing workflow with id {}", id)
        return try {
            val removed = workflows.removeIf { it.id == id }
            if (removed) {
                workflowRepository.delete(id)
            }
            removed
        } catch (e: Exception) {
            LOG.error("Unable to remove workflow", e)
            reload()
            false
        }
    }

    @Synchronized
    private fun reload() {
        workflows.clear()
        workflows.addAll(workflowRepository.getAll())
    }
}