package com.robert.persistance

import com.robert.DBConnector
import com.robert.LBAlgorithms
import com.robert.Workflow
import com.robert.StorageUtils
import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ServerException
import java.sql.Types
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class WorkflowStorage {
    fun get(id: String): Workflow {
        val query = """
            SELECT id, image, memory_limit, min_deployments, max_deployments, algorithm, wm.path, wm.port FROM workflows 
            INNER JOIN workflow_mappings wm on workflows.id = wm.workflow_id where id = ?
        """.trimIndent()

        DBConnector.getConnection()
            .prepareStatement(query)
            .use { st ->
                st.setString(1, id)
                st.executeQuery().use { rs ->
                    if (rs.next()) {
                        val image = rs.getString("image")
                        var memoryLimit: Long? = rs.getLong("memory_limit")
                        if (memoryLimit == 0L) {
                            memoryLimit = null
                        }
                        var minDeployments: Int? = rs.getInt("min_deployments")
                        if (minDeployments == 0) {
                            minDeployments = null
                        }
                        var maxDeployments: Int? = rs.getInt("max_deployments")
                        if (maxDeployments == 0) {
                            maxDeployments = null
                        }
                        val algorithm = LBAlgorithms.valueOf(rs.getString("algorithm"))
                        val pathMapping = HashMap<String, Int>()
                        pathMapping[rs.getString("path")] = rs.getInt("port")
                        while (rs.next()) {
                            pathMapping[rs.getString("path")] = rs.getInt("port")
                        }
                        return Workflow(id, image, memoryLimit, minDeployments, maxDeployments, algorithm, pathMapping)
                    }
                }
            }

        throw NotFoundException()
    }

    fun getAll(): List<Workflow> {
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

    fun add(
        image: String,
        memoryLimit: Long?,
        minDeployments: Int?,
        maxDeployments: Int?,
        algorithm: LBAlgorithms,
        pathMapping: Map<String, Int>
    ): Workflow {
        val id = UUID.randomUUID().toString()

        DBConnector.getTransactionConnection().use { conn ->
            try {
                conn.prepareStatement("INSERT INTO workflows(id, image, memory_limit, min_deployments, max_deployments, algorithm) VALUES (?, ?, ?, ?, ?, ?)")
                    .use { st ->
                        st.setString(1, id)
                        st.setString(2, image)
                        if (memoryLimit != null) {
                            st.setLong(3, memoryLimit)
                        } else {
                            st.setNull(3, Types.NUMERIC)
                        }
                        if (minDeployments != null) {
                            st.setInt(4, minDeployments)
                        } else {
                            st.setNull(4, Types.NUMERIC)
                        }
                        if (maxDeployments != null) {
                            st.setInt(5, maxDeployments)
                        } else {
                            st.setNull(5, Types.NUMERIC)
                        }
                        st.setString(6, algorithm.value)
                        StorageUtils.executeInsert(st)
                    }
                for (mapping in pathMapping.entries) {
                    conn.prepareStatement("INSERT INTO workflow_mappings(path, workflow_id, port) VALUES (?, ?, ?)")
                        .use { st ->
                            st.setString(1, mapping.key)
                            st.setString(2, id)
                            st.setInt(3, mapping.value)
                            StorageUtils.executeInsert(st)
                        }
                }
                conn.commit()
            } catch (_: Exception) {
                conn.rollback()
                throw ServerException()
            }
        }

        return Workflow(id, image, memoryLimit, minDeployments, maxDeployments, algorithm, pathMapping)
    }

    fun update(id: String, minDeployments: Int?, maxDeployments: Int?, algorithm: LBAlgorithms?) {
        var newMinDeployments: Int? = null
        var newMaxDeployments: Int? = null
        var newAlgorithm: LBAlgorithms? = null

        if (minDeployments == null || maxDeployments == null || algorithm == null) {
            DBConnector.getConnection()
                .prepareStatement("SELECT min_deployments, max_deployments, algorithm FROM workflows WHERE id = ?")
                .use { st ->
                    st.executeQuery()
                        .use { rs ->
                            newMinDeployments = minDeployments ?: rs.getInt("min_deployments")
                            if (newMinDeployments == 0) {
                                newMinDeployments = null
                            }
                            newMaxDeployments = maxDeployments ?: rs.getInt("max_deployments")
                            if (newMaxDeployments == 0) {
                                newMaxDeployments = null
                            }
                            newAlgorithm = algorithm ?: LBAlgorithms.valueOf(rs.getString("algorithm"))
                        }
                }
        }

        DBConnector.getConnection()
            .prepareStatement("UPDATE workflows SET min_deployments = ?, max_deployments = ?, algorithm = ? WHERE id = ?")
            .use { st ->
                if (newMinDeployments != null) {
                    st.setInt(1, newMinDeployments!!)
                } else {
                    st.setNull(1, Types.NUMERIC)
                }
                if (newMaxDeployments != null) {
                    st.setInt(2, newMaxDeployments!!)
                } else {
                    st.setNull(2, Types.NUMERIC)
                }
                st.setString(3, newAlgorithm!!.value)
                st.setString(4, id)
                StorageUtils.executeUpdate(st)
            }
    }

    fun delete(id: String) {
        DBConnector.getConnection().prepareStatement("DELETE FROM workflows WHERE id = ?")
            .use { st ->
                st.setString(1, id)
                StorageUtils.executeUpdate(st)
            }
    }
}
