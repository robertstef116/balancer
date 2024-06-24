package com.robert.storage.entities

import com.robert.scaller.WorkerState
import org.jetbrains.exposed.sql.Table

object Workers: Table() {
    val id = uuid("id").autoGenerate()
    val alias = varchar("alias", 50)
    val status = enumerationByName<WorkerState>("status", 10)

    override val primaryKey = PrimaryKey(id)
}
