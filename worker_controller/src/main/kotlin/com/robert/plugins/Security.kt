package com.robert.plugins

import io.ktor.auth.*
import io.ktor.util.*
import io.ktor.application.*

fun Application.configureSecurity() {
    authentication {
        val appRealm = environment.config.property("authentication.realm").getString()
        val username = environment.config.property("authentication.username").getString()
        val password = environment.config.property("authentication.password").getString()
        val usersInMyRealmToHA1: Map<String, ByteArray> = mapOf(
            username to hex(password)
        )

        digest("digest_auth") {
            realm = appRealm
            algorithmName = "SHA-256"
            digestProvider { userName, realm ->
                usersInMyRealmToHA1[userName]
            }
        }
    }
}
