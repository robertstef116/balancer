package com.robert.persistance

import com.robert.DBConnector
import com.robert.Deployment
import com.robert.Storage
import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ServerException
import java.sql.Types
import java.util.*
import kotlin.collections.ArrayList

class DeploymentStorage {
    fun get(id: String): Deployment {
        val st = DBConnector.getConnection()
            .prepareStatement("SELECT id, path, image, memory_limit, ports FROM deployments WHERE id = ?")

        st.use {
            st.setString(1, id)
            val rs = st.executeQuery()
            if (rs.next()) {
                return Deployment(
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

    fun getAll(): List<Deployment> {
        val query = "SELECT id, path, image, memory_limit, ports FROM deployments order by image"
        val st = DBConnector.getConnection().createStatement()
        val deployments = ArrayList<Deployment>()

        st.use {
            val rs = st.executeQuery(query)
            rs.use {
                while (rs.next()) {
                    deployments.add(
                        Deployment(
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

        return deployments
    }

    fun add(path: String, image: String, memoryLimit: Long?, ports: List<Int>?): Deployment {
        val id = UUID.randomUUID().toString()

        val conn = DBConnector.getConnection()

        val st = conn.prepareStatement("INSERT INTO deployments(id, path, image, memory_limit, ports) VALUES (?, ?, ?, ?)")

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
                return Deployment(id, path, image, memoryLimit, ports?: emptyList())
            }
        }

        throw ServerException()
    }

    fun update(id: String, path: String?, image: String?, memoryLimit: Long?, ports: List<Int>?) {
        var deployment: Deployment? = null
        if (image == null || memoryLimit == null || ports == null) {
            deployment = get(id)
        }

        val conn = DBConnector.getConnection()
        val st = conn.prepareStatement("UPDATE deployments SET path = ?, image = ?, memory_limit = ?, ports = ? WHERE id = ?")

        st.use {
            st.setString(5, id)
            st.setString(1, path?:deployment!!.path)
            st.setString(2, image?:deployment!!.image)

            val memoryLimitValue = memoryLimit?:deployment!!.memoryLimit
            if (memoryLimitValue != null) {
                st.setLong(3, memoryLimitValue)
            } else {
                st.setNull(3, Types.NUMERIC)
            }

            val portsValue = ports?: deployment!!.ports
            st.setArray(4, conn.createArrayOf("INTEGER", portsValue.toTypedArray()))
            Storage.executeUpdate(st)
        }
    }

    fun delete(id: String) {
        val st = DBConnector.getConnection().prepareStatement("DELETE FROM deployments WHERE id = ?")

        st.use {
            st.setString(1, id)
            Storage.executeUpdate(st)
        }
    }
}
