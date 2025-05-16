package ru.mirea

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    val config = environment.config
    val jwtAudience = config.property("postgres.audience").getString()
    val jwtDomain = config.property("postgres.domain").getString()
    val jwtRealm = config.property("postgres.realm").getString()
    val jwtSecret = "secret"

    install(Authentication) {
        jwt {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience))
                    UserIdPrincipal(credential.payload.subject)
                else null
            }
        }
    }
}
