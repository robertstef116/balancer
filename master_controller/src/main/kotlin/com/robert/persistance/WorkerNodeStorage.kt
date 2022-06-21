package com.robert.persistance

import com.robert.DBConnector
import com.robert.StorageUtils
import com.robert.resources.Worker
import com.robert.enums.WorkerStatus
import com.robert.exceptions.NotFoundException
import com.robert.exceptions.ServerException
import java.util.*
import kotlin.collections.ArrayList

class WorkerNodeStorage {
    fun get(id: String): Worker {
        DBConnector.getConnection().prepareStatement("SELECT id, alias, host, port, status FROM workers WHERE id = ?")
            .use { st ->
                st.setString(1, id)
                st.executeQuery()
                    .use { rs ->
                        if (rs.next()) {
                            return Worker(
                                rs.getString("id"),
                                rs.getString("alias"),
                                rs.getString("host"),
                                rs.getInt("port"),
                                WorkerStatus.valueOf(rs.getString("status"))
                            )
                        }
                    }
            }

        throw NotFoundException()
    }

    fun getAll(): List<Worker> {
        val query = "SELECT id, alias, host, port, status FROM workers order by alias"
        val workers = ArrayList<Worker>()

        DBConnector.getConnection().createStatement()
            .use { st ->
                st.executeQuery(query)
                    .use { rs ->
                        while (rs.next()) {
                            workers.add(
                                Worker(
                                    rs.getString("id"),
                                    rs.getString("alias"),
                                    rs.getString("host"),
                                    rs.getInt("port"),
                                    WorkerStatus.valueOf(rs.getString("status"))
                                )
                            )
                        }
                    }
            }

        return workers
    }

    fun add(alias: String, host: String, port: Int): Worker {
        val id = UUID.randomUUID().toString()

        DBConnector.getConnection()
            .prepareStatement("INSERT INTO workers(id, alias, host, port, status) VALUES (?, ?, ?, ?, ?)")
            .use { st ->
                val status = WorkerStatus.STARTING
                st.setString(1, id)
                st.setString(2, alias)
                st.setString(3, host)
                st.setInt(4, port)
                st.setString(5, status.toString())
                val res = st.executeUpdate()

                if (res > 0) {
                    return Worker(id, alias, host, port, status)
                }
            }

        throw ServerException()
    }

    fun update(id: String, alias: String?, port: Int?, status: WorkerStatus?) {
        var worker: Worker? = null
        if (alias == null || status == null || port == null) {
            worker = get(id)
        }
        DBConnector.getConnection()
            .prepareStatement("UPDATE workers SET alias = ?, status = ?, port = ? WHERE id = ?")
            .use { st ->
                st.setString(4, id)
                st.setString(1, alias ?: worker!!.alias)
                st.setString(2, (status ?: worker!!.status).toString())
                st.setInt(3, port ?: worker!!.port)
                StorageUtils.executeUpdate(st)
            }
    }

    fun flipStatus(id: String) {
        DBConnector.getTransactionConnection().use { conn ->
            val status: WorkerStatus
            conn.prepareStatement("SELECT status FROM workers WHERE id = ?").use { st ->
                st.setString(1, id)
                st.executeQuery().use { rs ->
                    if (!rs.next()) {
                        throw NotFoundException()
                    }
                    status = WorkerStatus.valueOf(rs.getString("status"))
                }
            }
            conn.prepareStatement("UPDATE workers SET status = ? WHERE id = ?").use { st ->
                if (status == WorkerStatus.STARTED || status == WorkerStatus.STARTING) {
                    st.setString(1, WorkerStatus.STOPPING.toString())
                } else {
                    st.setString(1, WorkerStatus.STARTING.toString())
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
