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
            SELECT id, image, memory_limit, algorithm, wm.path, wm.port FROM workflows 
            INNER JOIN workflow_mappings wm on workflows.id = wm.workflow_id where id = ?
        """.trimIndent()

        DBConnector.getConnection()
            .prepareStatement(query)
            .use { st ->
                st.setString(1, id)
                st.executeQuery().use { rs ->
                    if (rs.next()) {
                        val image = rs.getString("image")
                        val memoryLimit = rs.getLong("memory_limit")
                        val algorithm = LBAlgorithms.valueOf(rs.getString("algorithm"))
                        val pathMapping = HashMap<String, Int>()
                        pathMapping[rs.getString("path")] = rs.getInt("port")
                        while (rs.next()) {
                            pathMapping[rs.getString("path")] = rs.getInt("port")
                        }
                        return Workflow(id, image, memoryLimit, algorithm, pathMapping)
                    }
                }
            }

        throw NotFoundException()
    }

    fun getAll(): List<Workflow> {
        val query = """
            SELECT id, image, memory_limit, algorithm, wm.path, wm.port FROM workflows 
            INNER JOIN workflow_mappings wm on workflows.id = wm.workflow_id order by id
        """.trimIndent()
        val workflows = ArrayList<Workflow>()

        DBConnector.getConnection().createStatement().use { st ->
            st.executeQuery(query)
                .use { rs ->
                    var id: String? = null
                    var image: String? = null
                    var memoryLimit: Long? = null
                    var algorithm: LBAlgorithms? = null
                    var pathMapping: MutableMap<String, Int>? = null
                    var currentId: String?

                    while (rs.next()) {
                        currentId = rs.getString("id")
                        if (currentId != id) {
                            if (id != null) {
                                workflows.add(Workflow(id, image!!, memoryLimit, algorithm!!, pathMapping!!))
                            }
                            id = currentId
                            image = rs.getString("image")
                            memoryLimit = rs.getLong("memory_limit")
                            algorithm = LBAlgorithms.valueOf(rs.getString("algorithm"))
                            pathMapping = HashMap()
                        }
                        pathMapping!![rs.getString("path")] = rs.getInt("port")
                    }
                    if (id != null) {
                        workflows.add(Workflow(id, image!!, memoryLimit, algorithm!!, pathMapping!!))
                    }
                }
        }

        return workflows
    }

    fun add(image: String, memoryLimit: Long?, algorithm: LBAlgorithms, pathMapping: Map<String, Int>): Workflow {
        val id = UUID.randomUUID().toString()

        DBConnector.getTransactionConnection().use { conn ->
            try {
                conn.prepareStatement("INSERT INTO workflows(id, image, memory_limit, algorithm) VALUES (?, ?, ?, ?)")
                    .use { st ->
                        st.setString(1, id)
                        st.setString(2, image)
                        if (memoryLimit != null) {
                            st.setLong(3, memoryLimit)
                        } else {
                            st.setNull(3, Types.NUMERIC)
                        }
                        st.setString(4, algorithm.value)
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
            } catch (_: Exception) {
                conn.rollback()
                throw ServerException()
            }
        }

        return Workflow(id, image, memoryLimit, algorithm, pathMapping)
    }

    fun update(id: String, memoryLimit: Long?, algorithm: LBAlgorithms?) {
        var currentMemoryLimit: Long? = null
        lateinit var currentAlgorithm: LBAlgorithms

        if (memoryLimit == null || algorithm == null) {
            DBConnector.getConnection()
                .prepareStatement("SELECT memory_limit, algorithm FROM workflows WHERE id = ?")
                .use { st ->
                    st.executeQuery()
                        .use { rs ->
                            currentMemoryLimit = rs.getLong("memory_limit")
                            if (currentMemoryLimit == 0L) {
                                currentMemoryLimit = null
                            }
                            currentAlgorithm = LBAlgorithms.valueOf(rs.getString("algorithm"))
                        }
                }
        }

        DBConnector.getConnection()
            .prepareStatement("UPDATE workflows SET memory_limit = ?, algorithm = ? WHERE id = ?")
            .use { st ->
                st.setString(3, id)

                when {
                    memoryLimit != null -> st.setLong(2, memoryLimit)
                    currentMemoryLimit != null -> st.setLong(2, currentMemoryLimit!!)
                    else -> st.setNull(2, Types.NUMERIC)
                }

                st.setString(2, algorithm?.value ?: currentAlgorithm.value)
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
