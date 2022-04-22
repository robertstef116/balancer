package com.robert.persistance

import com.robert.DBConnector
import com.robert.Workflow
import com.robert.StorageUtils
import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ServerException
import java.sql.Types
import java.util.*
import kotlin.collections.ArrayList

class WorkflowStorage {
    fun get(id: String): Workflow {
        val st = DBConnector.getConnection()
            .prepareStatement("SELECT id, path, image, memory_limit, ports FROM workflows WHERE id = ?")

        st.use {
            st.setString(1, id)
            val rs = st.executeQuery()
            if (rs.next()) {
                return Workflow(
                    rs.getString("id"),
                    rs.getString("path"),
                    rs.getString("image"),
                    rs.getLong("memory_limit"),
                    (rs.getArray("ports") as? Array<Int>)?.toList() ?: emptyList()
                )
            }
        }

        throw NotFoundException()
    }

    fun getAll(): List<Workflow> {
        val query = "SELECT id, path, image, memory_limit, ports FROM workflows order by image"
        val st = DBConnector.getConnection().createStatement()
        val workflows = ArrayList<Workflow>()

        st.use {
            val rs = st.executeQuery(query)
            rs.use {
                while (rs.next()) {
                    workflows.add(
                        Workflow(
                            rs.getString("id"),
                            rs.getString("path"),
                            rs.getString("image"),
                            rs.getLong("memory_limit"),
                            (rs.getArray("ports") as? Array<Int>)?.toList() ?: emptyList()
                        )
                    )
                }
            }
        }

        return workflows
    }

    fun add(path: String, image: String, memoryLimit: Long?, ports: List<Int>?): Workflow {
        val id = UUID.randomUUID().toString()

        val conn = DBConnector.getConnection()

        val st = DBConnector.getConnection().prepareStatement("INSERT INTO workflows(id, path, image, memory_limit, ports) VALUES (?, ?, ?, ?)")

        st.use {
            st.setString(1, id)
            st.setString(2, path)
            st.setString(3, image)
            if (memoryLimit != null) {
                st.setLong(4, memoryLimit)
            } else {
                st.setNull(4, Types.NUMERIC)
            }
            if (ports != null) {
                st.setArray(5, conn.createArrayOf("INTEGER", ports.toTypedArray()))
            } else {
                st.setNull(5, Types.ARRAY)
            }
            val res = st.executeUpdate()

            if (res > 0) {
                return Workflow(id, path, image, memoryLimit, ports?: emptyList())
            }
        }

        throw ServerException()
    }

    fun update(id: String, path: String?, image: String?, memoryLimit: Long?, ports: List<Int>?) {
        var workflow: Workflow? = null
        if (image == null || memoryLimit == null || ports == null) {
            workflow = get(id)
        }

        val conn = DBConnector.getConnection()
        val st = conn.prepareStatement("UPDATE workflows SET path = ?, image = ?, memory_limit = ?, ports = ? WHERE id = ?")

        st.use {
            st.setString(5, id)
            st.setString(1, path?:workflow!!.path)
            st.setString(2, image?:workflow!!.image)

            val memoryLimitValue = memoryLimit?:workflow!!.memoryLimit
            if (memoryLimitValue != null) {
                st.setLong(3, memoryLimitValue)
            } else {
                st.setNull(3, Types.NUMERIC)
            }

            val portsValue = ports?: workflow!!.ports
            st.setArray(4, conn.createArrayOf("INTEGER", portsValue.toTypedArray()))
            StorageUtils.executeUpdate(st)
        }
    }

    fun delete(id: String) {
        val st = DBConnector.getConnection().prepareStatement("DELETE FROM workflows WHERE id = ?")

        st.use {
            st.setString(1, id)
            StorageUtils.executeUpdate(st)
        }
    }
}
