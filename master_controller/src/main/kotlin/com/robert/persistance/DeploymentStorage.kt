package com.robert.persistance

import com.robert.DBConnector
import com.robert.Deployment

class DeploymentStorage {
    fun getDeployments(): List<Deployment> {
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
}
