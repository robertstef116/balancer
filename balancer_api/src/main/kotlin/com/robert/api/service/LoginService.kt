package com.robert.api.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.robert.ConfigProperties
import com.robert.exceptions.AuthenticationException
import com.robert.storage.repository.UserRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class LoginService : KoinComponent {
    private val storage: UserRepository by inject()

    private val audience = ConfigProperties.getString("jwt.audience")
    private val issuer = ConfigProperties.getString("jwt.issuer")
    private val secret = ConfigProperties.getString("jwt.secret")
    private val expirationTime = ConfigProperties.getInteger("jwt.expirationTime") ?: 600

    fun login(username: String, password: String): String {
        if (!storage.findUser(username, password)) {
            throw AuthenticationException()
        }

        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + expirationTime * 1000)) // convert expiration time from s to ms
            .sign(Algorithm.HMAC256(secret))
    }
}
