package com.robert.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.*

fun Application.configureSecurity() {
    val appRealm = environment.config.property("authentication.realm").getString()
    val username = environment.config.property("authentication.username").getString()
    val password = environment.config.property("authentication.password").getString()

    authentication {
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
