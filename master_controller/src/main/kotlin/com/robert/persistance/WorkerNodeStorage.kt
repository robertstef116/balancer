package com.robert.persistance

import com.robert.DBConnector
import com.robert.Storage
import com.robert.WorkerNode
import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ServerException
import java.util.*
import kotlin.collections.ArrayList

class WorkerNodeStorage {
    fun get(id: String): WorkerNode {
        val st = DBConnector.getConnection().prepareStatement("SELECT id, alias, ip, in_use FROM workers WHERE id = ?")

        st.use {
            st.setString(1, id)
            val rs = st.executeQuery()
            if (rs.next()) {
                return WorkerNode(
                    rs.getString("id"),
                    rs.getString("alias"),
                    rs.getString("ip"),
                    rs.getBoolean("in_use")
                )
            }
        }

        throw NotFoundException()
    }

    fun getAll(): List<WorkerNode> {
        val query = "SELECT id, alias, ip, in_use FROM workers order by alias"
        val st = DBConnector.getConnection().createStatement()
        val workerNodes = ArrayList<WorkerNode>()

        st.use {
            val rs = st.executeQuery(query)
            rs.use {
                while (rs.next()) {
                    workerNodes.add(
                        WorkerNode(
                            rs.getString("id"),
                            rs.getString("alias"),
                            rs.getString("ip"),
                            rs.getBoolean("in_use")
                        )
                    )
                }
            }
        }

        return workerNodes
    }

    fun add(alias: String, ip: String, inUse: Boolean): WorkerNode {
        val id = UUID.randomUUID().toString()

        val st = DBConnector.getConnection()
            .prepareStatement("INSERT INTO workers(id, alias, ip, in_use) VALUES (?, ?, ?, ?)")

        st.use {
            st.setString(1, id)
            st.setString(2, alias)
            st.setString(3, ip)
            st.setBoolean(4, inUse)
            val res = st.executeUpdate()

            if (res > 0) {
                return WorkerNode(id, alias, ip, inUse)
            }
        }

        throw ServerException()
    }

    fun update(id: String, alias: String?, inUse: Boolean?) {
        var workerNode: WorkerNode? = null
        if (alias == null || inUse == null) {
            workerNode = get(id)
        }
        val st = DBConnector.getConnection().prepareStatement("UPDATE workers SET alias = ?, in_use = ? WHERE id = ?")

        st.use {
            st.setString(3, id)
            st.setString(1, alias?:workerNode!!.alias)
            st.setBoolean(2, inUse?:workerNode!!.inUse)
            Storage.executeUpdate(st)
        }
    }

    fun delete(id: String) {
        val st = DBConnector.getConnection().prepareStatement("DELETE FROM workers WHERE id = ?")

        st.use {
            st.setString(1, id)
            Storage.executeUpdate(st)
        }
    }
}
