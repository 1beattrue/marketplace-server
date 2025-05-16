package ru.mirea

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    val database: Database = connectToPostgres()
    val marketItemService = MarketItemService(database)
    val userService = UserService(database)

    routing {
        // Create market item
        post("/market_items") {
            val item = call.receive<MarketItem>()
            val id = marketItemService.create(item)
            call.respond(HttpStatusCode.Created, id)
        }

        // Read market item
        get("/market_items/{id}") {
            val id =
                call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID")
            val item = marketItemService.read(id)
            if (item != null) {
                call.respond(HttpStatusCode.OK, item)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Update market item
        put("/market_items/{id}") {
            val id =
                call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")
            val item = call.receive<MarketItem>()
            marketItemService.update(id, item)
            call.respond(HttpStatusCode.OK)
        }

        // Delete market item
        delete("/market_items/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                "Invalid ID"
            )
            marketItemService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }

    routing {
        route("/auth") {
            post("/register") {
                val user = call.receive<ExposedUser>()
                val existingUser = userService.readByEmail(user.email)
                if (existingUser != null) {
                    return@post call.respond(HttpStatusCode.Conflict, "User already exists")
                }

                val newUser = ExposedUser(user.name, user.email, user.password)
                val createdUser = userService.readByEmail(newUser.email) ?: return@post call.respond(HttpStatusCode.InternalServerError, "User creation failed")

                val jwt = userService.createJWT(createdUser)
                call.respond(HttpStatusCode.Created, mapOf("token" to jwt))
            }

            post("/login") {
                val user = call.receive<ExposedUser>()
                val existingUser = userService.readByEmail(user.email)
                if (existingUser == null || user.password != existingUser.password) {
                    return@post call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                }

                // Create JWT token
                val jwt = userService.createJWT(existingUser)
                call.respond(HttpStatusCode.OK, mapOf("token" to jwt))
            }
        }
    }
}

fun Application.connectToPostgres(): Database {
    val url = environment.config.property("postgres.url").getString()
    val user = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()

    log.info("Connecting to PostgreSQL at $url")
    return Database.connect(url, driver = "org.postgresql.Driver", user = user, password = password)
}
