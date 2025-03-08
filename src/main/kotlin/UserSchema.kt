package ru.mirea

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedUser(
    val name: String,
    val email: String,
    val password: String
)

class UserService(database: Database) {
    object Users : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", 255)
        val email = varchar("email", 255).uniqueIndex()
        val password = varchar("password", 255)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun create(user: ExposedUser): Int = dbQuery {
        Users.insert {
            it[name] = user.name
            it[email] = user.email
            it[password] = user.password
        }[Users.id]
    }

    suspend fun readByEmail(email: String): ExposedUser? = dbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .map {
                ExposedUser(
                    it[Users.name],
                    it[Users.email],
                    it[Users.password]
                )
            }
            .singleOrNull()
    }

    suspend fun update(id: Int, user: ExposedUser) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[name] = user.name
                it[email] = user.email
                it[password] = user.password
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Users.deleteWhere { Users.id.eq(id) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    fun createJWT(user: ExposedUser): String {
        val jwtSecret = "secret"
        val jwtAudience = "jwt-audience"
        val jwtDomain = "https://jwt-provider-domain/"

        return JWT.create()
            .withIssuer(jwtDomain)
            .withAudience(jwtAudience)
            .withClaim("name", user.name)
            .withClaim("email", user.email)
            .withSubject(user.email)
            .sign(Algorithm.HMAC256(jwtSecret))
    }
}

