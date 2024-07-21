package com.robert.storage.entities

import org.jetbrains.exposed.sql.Table

object Users: Table("users") {
    val username = varchar("username", 50)
    val password = varchar("password", 16)

    override val primaryKey = PrimaryKey(username)
}