package com.robert.persistance

import com.robert.scaller.WorkerStatusR
import org.jetbrains.exposed.sql.Table

object Workers: Table() {
    val id = uuid("id").autoGenerate()
    val alias = varchar("alias", 50)
    val host = varchar("host", 50)
    val health = integer("port")
    val status = enumerationByName<WorkerStatusR>("status", 10)

    override val primaryKey = PrimaryKey(id)
}
