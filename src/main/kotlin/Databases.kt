package ru.mirea

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    val database: Database = connectToPostgres()
    val marketItemService = MarketItemService(database)
    val userService = UserService(database)

    routing {
        authenticate {
            // Create market item
            post("/products") {
                val items = call.receive<List<MarketItem>>()
                val insertedIds = items.map { item ->
                    marketItemService.create(item)
                }
                call.respond(HttpStatusCode.Created, insertedIds)
            }

            // Read market items paging
            get("/products") {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val skip = call.request.queryParameters["skip"]?.toIntOrNull() ?: 0

                // Валидация limit и skip
                if (limit < 0 || skip < 0) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "limit and skip must be non-negative"
                    )
                    return@get
                }

                val items = marketItemService.getPaged(limit, skip)
                call.respond(RequestMarketItemsContainer(items))
            }

            get("/products/categories") {
                val categories = marketItemService.getAllCategories()
                call.respond(categories)
            }

            get("/products/category/{category}") {
                val category = call.parameters["category"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Wrong query params"
                )

                val items = marketItemService.getByCategory(category)
                call.respond(RequestMarketItemsContainer(items))
            }

            get("/products/search") {
                val query = call.queryParameters["q"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Wrong query params"
                )

                val items = marketItemService.search(query)
                call.respond(RequestMarketItemsContainer(items))
            }


            // Delete market item
            delete("/products/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    "Invalid ID"
                )
                marketItemService.delete(id)
                call.respond(HttpStatusCode.OK)
            }
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
                val createdUser = userService.readByEmail(newUser.email)
                    ?: return@post call.respond(HttpStatusCode.InternalServerError, "User creation failed")

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
