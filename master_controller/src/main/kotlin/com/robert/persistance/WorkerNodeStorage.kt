package com.robert.persistance

import com.robert.DBConnector
import com.robert.StorageUtils
import com.robert.WorkerNode
import com.robert.WorkerNodeStatus
import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ServerException
import java.util.*
import kotlin.collections.ArrayList

class WorkerNodeStorage {
    fun get(id: String): WorkerNode {
        DBConnector.getConnection().prepareStatement("SELECT id, alias, host, port, status FROM workers WHERE id = ?")
            .use { st ->
                st.setString(1, id)
                st.executeQuery()
                    .use { rs ->
                        if (rs.next()) {
                            return WorkerNode(
                                rs.getString("id"),
                                rs.getString("alias"),
                                rs.getString("host"),
                                rs.getInt("port"),
                                WorkerNodeStatus.valueOf(rs.getString("status"))
                            )
                        }
                    }
            }

        throw NotFoundException()
    }

    fun getAll(): List<WorkerNode> {
        val query = "SELECT id, alias, host, port, status FROM workers order by alias"
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
                                    rs.getInt("port"),
                                    WorkerNodeStatus.valueOf(rs.getString("status"))
                                )
                            )
                        }
                    }
            }

        return workerNodes
    }

    fun add(alias: String, host: String, port: Int): WorkerNode {
        val id = UUID.randomUUID().toString()

        DBConnector.getConnection()
            .prepareStatement("INSERT INTO workers(id, alias, host, port, status) VALUES (?, ?, ?, ?, ?)")
            .use { st ->
                val status = WorkerNodeStatus.STARTING
                st.setString(1, id)
                st.setString(2, alias)
                st.setString(3, host)
                st.setInt(4, port)
                st.setString(5, status.toString())
                val res = st.executeUpdate()

                if (res > 0) {
                    return WorkerNode(id, alias, host, port, status)
                }
            }

        throw ServerException()
    }

    fun update(id: String, alias: String?, port: Int?, status: WorkerNodeStatus?) {
        var workerNode: WorkerNode? = null
        if (alias == null || status == null || port == null) {
            workerNode = get(id)
        }
        DBConnector.getConnection()
            .prepareStatement("UPDATE workers SET alias = ?, status = ?, port = ? WHERE id = ?")
            .use { st ->
                st.setString(4, id)
                st.setString(1, alias ?: workerNode!!.alias)
                st.setString(2, (status ?: workerNode!!.status).toString())
                st.setInt(3, port ?: workerNode!!.port)
                StorageUtils.executeUpdate(st)
            }
    }

    fun flipStatus(id: String) {
        DBConnector.getTransactionConnection().use { conn ->
            val status: WorkerNodeStatus
            conn.prepareStatement("SELECT status FROM workers WHERE id = ?").use { st ->
                st.setString(1, id)
                st.executeQuery().use { rs ->
                    if (!rs.next()) {
                        throw NotFoundException()
                    }
                    status = WorkerNodeStatus.valueOf(rs.getString("status"))
                }
            }
            conn.prepareStatement("UPDATE workers SET status = ? WHERE id = ?").use { st ->
                if (status == WorkerNodeStatus.STARTED || status == WorkerNodeStatus.STARTING) {
                    st.setString(1, WorkerNodeStatus.STOPPING.toString())
                } else {
                    st.setString(1, WorkerNodeStatus.STARTING.toString())
                }
                st.setString(2, id)
                StorageUtils.executeUpdate(st)
            }
            conn.commit()
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
