package com.robert.storage.repository

interface UserRepository {
    fun findUser(username: String, password: String): Boolean
}