package com.robert.storage.repository.exposed

import com.robert.storage.entities.Users
import com.robert.storage.repository.UserRepository
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepositoryImpl : UserRepository {
    override fun findUser(username: String, password: String): Boolean = transaction {
        Users.selectAll()
            .where { (Users.username eq username) and (Users.password eq password) }
            .count() == 1L
    }
}