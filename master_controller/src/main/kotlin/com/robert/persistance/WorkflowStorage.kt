package com.robert.persistance

import com.robert.DBConnector
import com.robert.enums.LBAlgorithms
import com.robert.resources.Workflow
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
                        var upScaling: Int? = rs.getInt("up_scaling")
                        if (upScaling == 0) {
                            upScaling = null
                        }
                        var downScaling: Int? = rs.getInt("down_scaling")
                        if (upScaling == 0) {
                            downScaling = null
                        }
                        val algorithm = LBAlgorithms.valueOf(rs.getString("algorithm"))
                        val pathMapping = HashMap<String, Int>()
                        pathMapping[rs.getString("path")] = rs.getInt("port")
                        while (rs.next()) {
                            pathMapping[rs.getString("path")] = rs.getInt("port")
                        }
                        return Workflow(id, image, memoryLimit, minDeployments, maxDeployments, upScaling, downScaling, algorithm, pathMapping)
                    }
                }
            }

        throw NotFoundException()
    }

    fun getAll(): List<Workflow> {
        val query = """
            SELECT id, image, memory_limit, min_deployments, max_deployments, up_scaling, down_scaling, algorithm, wm.path, wm.port FROM workflows 
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
                    var upScaling: Int? = null
                    var downScaling: Int? = null
                    var algorithm: LBAlgorithms? = null
                    var pathMapping: MutableMap<String, Int>? = null
                    var currentId: String?

                    while (rs.next()) {
                        currentId = rs.getString("id")
                        if (currentId != id) {
                            if (id != null) {
                                workflows.add(
                                    Workflow(
                                        id, image!!, memoryLimit, minDeployments, maxDeployments,
                                        upScaling, downScaling, algorithm!!, pathMapping!!
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
                            upScaling = rs.getInt("up_scaling")
                            if (upScaling == 0) {
                                upScaling = null
                            }
                            downScaling = rs.getInt("down_scaling")
                            if (downScaling == 0) {
                                downScaling = null
                            }
                            algorithm = LBAlgorithms.valueOf(rs.getString("algorithm"))
                            pathMapping = HashMap()
                        }
                        pathMapping!![rs.getString("path")] = rs.getInt("port")
                    }
                    if (id != null) {
                        workflows.add(
                            Workflow(
                                id, image!!, memoryLimit, minDeployments, maxDeployments,
                                upScaling, downScaling, algorithm!!, pathMapping!!
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
        upScaling: Int?,
        downScaling: Int?,
        algorithm: LBAlgorithms,
        pathMapping: Map<String, Int>
    ): Workflow {
        val id = UUID.randomUUID().toString()

        DBConnector.getTransactionConnection().use { conn ->
            try {
                conn.prepareStatement("INSERT INTO workflows(id, image, memory_limit, min_deployments, max_deployments, up_scaling, down_scaling, algorithm) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
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
                        if (upScaling != null) {
                            st.setInt(6, upScaling)
                        } else {
                            st.setNull(6, Types.NUMERIC)
                        }
                        if (downScaling != null) {
                            st.setInt(7, downScaling)
                        } else {
                            st.setNull(7, Types.NUMERIC)
                        }
                        st.setString(8, algorithm.value)
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

        return Workflow(id, image, memoryLimit, minDeployments, maxDeployments, upScaling, downScaling, algorithm, pathMapping)
    }

    fun update(id: String, minDeployments: Int?, maxDeployments: Int?, upScaling: Int?, downScaling: Int?, algorithm: LBAlgorithms?) {
        var newMinDeployments: Int? = null
        var newMaxDeployments: Int? = null
        var newUpScaling: Int? = null
        var newDownScaling: Int? = null
        var newAlgorithm: LBAlgorithms? = null

        if (minDeployments == null || maxDeployments == null || upScaling == null || downScaling == null || algorithm == null) {
            DBConnector.getConnection()
                .prepareStatement("SELECT min_deployments, max_deployments, up_scaling, down_scaling, algorithm FROM workflows WHERE id = ?")
                .use { st ->
                    st.setString(1, id)
                    st.executeQuery()
                        .use { rs ->
                            if (!rs.next()) {
                                throw NotFoundException()
                            }
                            newMinDeployments = minDeployments ?: rs.getInt("min_deployments")
                            if (newMinDeployments == 0) {
                                newMinDeployments = null
                            }
                            newMaxDeployments = maxDeployments ?: rs.getInt("max_deployments")
                            if (newMaxDeployments == 0) {
                                newMaxDeployments = null
                            }
                            newUpScaling = maxDeployments ?: rs.getInt("up_scaling")
                            if (newUpScaling == 0) {
                                newUpScaling = null
                            }
                            newDownScaling = maxDeployments ?: rs.getInt("down_scaling")
                            if (newDownScaling == 0) {
                                newDownScaling = null
                            }
                            newAlgorithm = algorithm ?: LBAlgorithms.valueOf(rs.getString("algorithm"))
                        }
                }
        }

        DBConnector.getConnection()
            .prepareStatement("UPDATE workflows SET min_deployments = ?, max_deployments = ?, up_scaling = ?, down_scaling = ?, algorithm = ? WHERE id = ?")
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
                if (upScaling != null) {
                    st.setInt(3, upScaling)
                } else {
                    st.setNull(3, Types.NUMERIC)
                }
                if (downScaling != null) {
                    st.setInt(4, downScaling)
                } else {
                    st.setNull(4, Types.NUMERIC)
                }
                st.setString(5, newAlgorithm!!.value)
                st.setString(6, id)
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
