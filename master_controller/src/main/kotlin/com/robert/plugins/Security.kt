package com.robert.plugins

import io.ktor.auth.*
import io.ktor.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*

fun Application.configureSecurity() {
    authentication {
        jwt("auth-jwt") {
            val audience = environment.config.property("jwt.audience").getString()
            val issuer = environment.config.property("jwt.issuer").getString()
            val secret = environment.config.property("jwt.secret").getString()
            realm = environment.config.property("jwt.realm").getString()
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(audience) &&
                    credential.payload.getClaim("username").asString() != ""
                )
                    JWTPrincipal(credential.payload) else null
            }
        }
    }
}
