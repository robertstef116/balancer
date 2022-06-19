package com.robert

import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ServerException
import com.robert.exceptions.WorkflowAnalyticsEvent
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Storage {
    companion object {
        private val log = LoggerFactory.getLogger(Storage::class.java)
    }

    fun getWorkers(status: WorkerNodeStatus = WorkerNodeStatus.STARTED): List<WorkerNode> {
        log.debug("get workers")
        val query = "SELECT id, alias, host, port FROM workers where status='$status'"
        val workerNodes = ArrayList<WorkerNode>()

        DBConnector.getConnection().createStatement().use { st ->
            st.executeQuery(query)
                .use { rs ->
                    while (rs.next()) {
                        workerNodes.add(
                            WorkerNode(
                                rs.getString("id"),
                                rs.getString("alias"),
                                rs.getString("host"),
                                rs.getInt("port"),
                                status
                            )
                        )
                    }
                }
        }

        return workerNodes
    }

    fun disableWorker(id: String, status: WorkerNodeStatus = WorkerNodeStatus.STOPPED) {
        log.debug("disable worker with id {}, new status will be {}", id, status)
        DBConnector.getTransactionConnection().use { conn ->
            try {
                conn.prepareStatement("UPDATE workers set status = ? WHERE id = ?").use { st ->
                    st.setString(1, status.toString())
                    st.setString(2, id)
                    StorageUtils.executeUpdate(st)
                }
                conn.prepareStatement("DELETE FROM deployments WHERE worker_id = ?").use { st ->
                    st.setString(1, id)
                    StorageUtils.executeUpdate(st)
                }
                conn.commit()
            } catch (_: Exception) {
                conn.rollback()
                throw ServerException()
            }
        }
    }

    fun enableWorker(id: String) {
        log.debug("enable worker with id {}", id)
        DBConnector.getConnection().prepareStatement("UPDATE workers set status = ? WHERE id = ?").use { st ->
            st.setString(1, WorkerNodeStatus.STARTED.toString())
            st.setString(2, id)
            StorageUtils.executeUpdate(st)
        }
    }

    fun getPathsMapping(): Map<WorkflowPath, List<PathTargetResource>> {
        log.debug("get paths mapping")
        val query = """
            SELECT d.id, d.worker_id, d.workflow_id, wm.path, w.host, worker_port, w2.algorithm FROM deployment_mappings
                INNER JOIN deployments d ON d.id = deployment_mappings.deployment_id
                INNER JOIN workers w ON w.id = d.worker_id
                INNER JOIN workflow_mappings wm ON d.workflow_id = wm.workflow_id AND deployment_port = wm.port
                INNER JOIN workflows w2 on w2.id = d.workflow_id
                    ORDER BY wm.path
        """.trimIndent()

        val pathsMapping = HashMap<WorkflowPath, List<PathTargetResource>>()
        DBConnector.getConnection().createStatement().use { st ->
            st.executeQuery(query)
                .use { rs ->
                    var path: String? = null
                    var currentPath: String?
                    var algorithm: LBAlgorithms? = null
                    var mappingList: MutableList<PathTargetResource>? = null

                    while (rs.next()) {
                        currentPath = rs.getString("path")
                        if (currentPath != path) {
                            if (path != null) {
                                pathsMapping[WorkflowPath(path, algorithm!!)] = mappingList!!
                            }
                            path = currentPath
                            algorithm = LBAlgorithms.valueOf(rs.getString("algorithm"))
                            mappingList = ArrayList()
                        }
                        mappingList!!.add(
                            PathTargetResource(
                                rs.getString("worker_id"),
                                rs.getString("workflow_id"),
                                rs.getString("id"),
                                rs.getString("host"),
                                rs.getInt("worker_port")
                            )
                        )
                    }

                    if (path != null) {
                        pathsMapping[WorkflowPath(path, algorithm!!)] = mappingList!!
                    }
                }
        }

        return pathsMapping
    }

    fun getWorkflows(): List<Workflow> {
        log.debug("get workflows")
        val query = """
            SELECT id, image, memory_limit, min_deployments, max_deployments, algorithm, wm.path, wm.port FROM workflows 
            INNER JOIN workflow_mappings wm on workflows.id = wm.workflow_id order by id
        """.trimIndent()
        val workflows = ArrayList<Workflow>()

        DBConnector.getConnection().createStatement().use { st ->
            st.executeQuery(query)
                .use { rs ->
                    var id: String? = null
                    var image: String? = null
                    var memoryLimit: Long? = null
                    var minDeployments: Int? = null
                    var maxDeployments: Int? = null
                    var algorithm: LBAlgorithms? = null
                    var pathMapping: MutableMap<String, Int>? = null
                    var currentId: String?

                    while (rs.next()) {
                        currentId = rs.getString("id")
                        if (currentId != id) {
                            if (id != null) {
                                workflows.add(
                                    Workflow(
                                        id, image!!, memoryLimit, minDeployments,
                                        maxDeployments, algorithm!!, pathMapping!!
                                    )
                                )
                            }
                            id = currentId
                            image = rs.getString("image")
                            memoryLimit = rs.getLong("memory_limit")
                            if (memoryLimit == 0L) {
                                memoryLimit = null
                            }
                            minDeployments = rs.getInt("min_deployments")
                            if (minDeployments == 0) {
                                minDeployments = null
                            }
                            maxDeployments = rs.getInt("max_deployments")
                            if (maxDeployments == 0) {
                                maxDeployments = null
                            }
                            algorithm = LBAlgorithms.valueOf(rs.getString("algorithm"))
                            pathMapping = HashMap()
                        }
                        pathMapping!![rs.getString("path")] = rs.getInt("port")
                    }
                    if (id != null) {
                        workflows.add(
                            Workflow(
                                id, image!!, memoryLimit, minDeployments,
                                maxDeployments, algorithm!!, pathMapping!!
                            )
                        )
                    }
                }
        }

        return workflows
    }

    fun getDeployments(): List<Deployment> {
        log.debug("get deployments")
        val query = """
            SELECT id, worker_id, workflow_id, container_id, timestamp, dm.worker_port, dm.deployment_port FROM deployments 
            INNER JOIN deployment_mappings dm on deployments.id = dm.deployment_id order by id
       """.trimIndent()
        val deployments = ArrayList<Deployment>()

        DBConnector.getConnection().createStatement().use { st ->
            st.executeQuery(query)
                .use { rs ->
                    var id: String? = null
                    var workerId: String? = null
                    var workflowId: String? = null
                    var containerId: String? = null
                    var timestamp: Long? = null
                    var portsMapping: MutableMap<Int, Int>? = null
                    var currentId: String?

                    while (rs.next()) {
                        currentId = rs.getString("id")
                        if (currentId != id) {
                            if (id != null) {
                                deployments.add(
                                    Deployment(
                                        id,
                                        workerId!!,
                                        workflowId!!,
                                        containerId!!,
                                        timestamp!!,
                                        portsMapping!!
                                    )
                                )
                            }
                            id = currentId
                            workerId = rs.getString("worker_id")
                            workflowId = rs.getString("workflow_id")
                            containerId = rs.getString("container_id")
                            timestamp = rs.getLong("timestamp")
                            portsMapping = HashMap()
                        }
                        portsMapping!![rs.getInt("worker_port")] = rs.getInt("deployment_port")
                    }
                    if (id != null) {
                        deployments.add(
                            Deployment(
                                id,
                                workerId!!,
                                workflowId!!,
                                containerId!!,
                                timestamp!!,
                                portsMapping!!
                            )
                        )
                    }
                }
        }

        return deployments
    }

    fun addDeployment(id: String?, workerId: String, workflowId: String, containerId: String, portsMapping: Map<Int, Int>): Deployment {
        log.debug("adding deployment on worker {}", workerId)
        val deploymentId = id ?: UUID.randomUUID().toString()
        val timestamp = Instant.now().epochSecond

        DBConnector.getTransactionConnection().use { conn ->
            try {
                conn.prepareStatement("INSERT INTO deployments(id, worker_id, workflow_id, container_id, timestamp) VALUES (?, ?, ?, ?, ?)")
                    .use { st ->
                        st.setString(1, deploymentId)
                        st.setString(2, workerId)
                        st.setString(3, workflowId)
                        st.setString(4, containerId)
                        st.setLong(5, timestamp)
                        StorageUtils.executeInsert(st)
                    }
                for (mapping in portsMapping.entries) {
                    conn.prepareStatement("INSERT INTO deployment_mappings(deployment_id, deployment_port, worker_port) VALUES (?, ?, ?)")
                        .use { st ->
                            st.setString(1, deploymentId)
                            st.setInt(2, mapping.value)
                            st.setInt(3, mapping.key)
                            StorageUtils.executeInsert(st)
                        }
                }
                conn.prepareStatement("INSERT INTO workflow_analytics(worker_id, workflow_id, deployment_id, event, timestamp) VALUES (?, ?, ?, ?, ?)")
                    .use { st ->
                        st.setString(1, workerId)
                        st.setString(2, workflowId)
                        st.setString(3, deploymentId)
                        st.setString(4, WorkflowAnalyticsEvent.ADD.value)
                        st.setLong(5, timestamp)
                        StorageUtils.executeInsert(st)
                    }
                conn.commit()
            } catch (e: Exception) {
                log.error("error adding deployment, err = {}", e.message)
                conn.rollback()
                throw ServerException()
            }
        }

        return Deployment(deploymentId, workerId, workflowId, containerId, timestamp, portsMapping)
    }

    fun updateDeploymentMapping(id: String?, portsMapping: Map<Int, Int>) {
        log.debug("update deployment data")
        DBConnector.getTransactionConnection().use { conn ->
            try {
                for (mapping in portsMapping.entries) {
                    conn.prepareStatement("UPDATE deployment_mappings SET worker_port = ? WHERE deployment_id = ? AND deployment_port = ?")
                        .use { st ->
                            st.setString(2, id)
                            st.setInt(3, mapping.value)
                            st.setInt(1, mapping.key)
                            StorageUtils.executeUpdate(st)
                        }
                }
                conn.commit()
            } catch (e: Exception) {
                log.error("error updating deployment mapping, {}", e.message)
                conn.rollback()
                throw ServerException()
            }
        }
    }

    fun deleteDeployment(id: String) {
        log.debug("delete deployment")
        DBConnector.getTransactionConnection().use { conn ->
            val timestamp = Instant.now().epochSecond
            log.debug("delete deployment with id {}", id)
            try {
                val workerId: String
                val workflowId: String
                conn.prepareStatement("SELECT worker_id, workflow_id FROM deployments WHERE id = ?")
                    .use { st ->
                        st.setString(1, id)
                        st.executeQuery().use { rs ->
                            if (rs.next()) {
                                workerId = rs.getString("worker_id")
                                workflowId = rs.getString("workflow_id")
                            } else {
                                throw NotFoundException()
                            }
                        }
                    }

                conn.prepareStatement("DELETE FROM deployments WHERE id = ?")
                    .use { st ->
                        st.setString(1, id)
                        StorageUtils.executeUpdate(st)
                    }

                conn.prepareStatement("INSERT INTO workflow_analytics(worker_id, workflow_id, deployment_id, event, timestamp) VALUES (?, ?, ?, ?, ?)")
                    .use { st ->
                        st.setString(1, workerId)
                        st.setString(2, workflowId)
                        st.setString(3, id)
                        st.setString(4, WorkflowAnalyticsEvent.REMOVE.value)
                        st.setLong(5, timestamp)
                        StorageUtils.executeInsert(st)
                    }
                conn.commit()
            } catch (e: NotFoundException) {
                throw e
            } catch (e: Exception) {
                log.error("error removing deployment, err = {}", e.message)
                conn.rollback()
                throw ServerException()
            }
        }
    }

    fun getConfigs(): Map<String, String> {
        log.debug("get configs")
        val query = "SELECT key, value FROM config"
        val configs = HashMap<String, String>()

        DBConnector.getConnection().createStatement()
            .use { st ->
                st.executeQuery(query)
                    .use { rs ->
                        while (rs.next()) {
                            configs[rs.getString("key")] = rs.getString("value")
                        }
                    }
            }

        return configs
    }

    fun persistAnalytics(workerId: String, workflowId: String, deploymentId: String, timestamp: Long) {
        log.debug("persist analytics")
        DBConnector.getConnection().prepareStatement("INSERT INTO analytics( worker_id, workflow_id, deployment_id, timestamp) VALUES (?, ?, ?, ?)")
            .use { st ->
                st.setString(1, workerId)
                st.setString(2, workflowId)
                st.setString(3, deploymentId)
                st.setLong(4, timestamp)
                StorageUtils.executeUpdate(st)
            }
    }

    fun getConfigChangeTimestampMetadata(): Map<String, Long> {
        log.debug("get config changes timestamp")
        val res = mutableMapOf<String, Long>()

        DBConnector.getConnection().createStatement().use { st ->
            st.executeQuery("SELECT key, value FROM metadata WHERE key like '%${Constants.UPDATE_AWARE_SERVICE_SUFFIX}'").use { rs ->
                while (rs.next()) {
                    res[rs.getString("key")] = rs.getLong("value")
                }
            }
        }

        return res
    }
}
