package com.robert.persistance

import com.robert.DBConnector
import com.robert.StorageUtils
import com.robert.WorkerNode
import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ServerException
import java.util.*
import kotlin.collections.ArrayList

class WorkerNodeStorage {
    fun get(id: String): WorkerNode {
        DBConnector.getConnection().prepareStatement("SELECT id, alias, host, in_use FROM workers WHERE id = ?")
            .use { st ->
                st.setString(1, id)
                st.executeQuery()
                    .use { rs ->
                        if (rs.next()) {
                            return WorkerNode(
                                rs.getString("id"),
                                rs.getString("alias"),
                                rs.getString("host"),
                                rs.getBoolean("in_use")
                            )
                        }
                    }
            }

        throw NotFoundException()
    }

    fun getAll(): List<WorkerNode> {
        val query = "SELECT id, alias, host, in_use FROM workers order by alias"
        val workerNodes = ArrayList<WorkerNode>()

        DBConnector.getConnection().createStatement()
            .use { st ->
                st.executeQuery(query)
                    .use { rs ->
                        while (rs.next()) {
                            workerNodes.add(
                                WorkerNode(
                                    rs.getString("id"),
                                    rs.getString("alias"),
                                    rs.getString("host"),
                                    rs.getBoolean("in_use")
                                )
                            )
                        }
                    }
            }

        return workerNodes
    }

    fun add(alias: String, host: String, inUse: Boolean): WorkerNode {
        val id = UUID.randomUUID().toString()

        DBConnector.getConnection()
            .prepareStatement("INSERT INTO workers(id, alias, host, in_use) VALUES (?, ?, ?, ?)")
            .use { st ->

                st.setString(1, id)
                st.setString(2, alias)
                st.setString(3, host)
                st.setBoolean(4, inUse)
                val res = st.executeUpdate()

                if (res > 0) {
                    return WorkerNode(id, alias, host, inUse)
                }
            }

        throw ServerException()
    }

    fun update(id: String, alias: String?, inUse: Boolean?) {
        var workerNode: WorkerNode? = null
        if (alias == null || inUse == null) {
            workerNode = get(id)
        }
        DBConnector.getConnection().prepareStatement("UPDATE workers SET alias = ?, in_use = ? WHERE id = ?")
            .use { st ->
                st.setString(3, id)
                st.setString(1, alias ?: workerNode!!.alias)
                st.setBoolean(2, inUse ?: workerNode!!.inUse)
                StorageUtils.executeUpdate(st)
            }
    }

    fun delete(id: String) {
        DBConnector.getConnection().prepareStatement("DELETE FROM workers WHERE id = ?")
            .use { st ->
                st.setString(1, id)
                StorageUtils.executeUpdate(st)
            }
    }
}
