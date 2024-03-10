package com.example.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedUser(val name: String, val age: Int)

object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", length = 50)
    val age = integer("age")

    override val primaryKey = PrimaryKey(id)
}


class UserService(database: Database) {
    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }


    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(user: ExposedUser): Int =
        dbQuery {
            Users.insert {
                it[name] = user.name
                it[age] = user.age
            }[Users.id]
        }

    suspend fun read(id: Int): ExposedUser? =
        dbQuery {
            Users.select { Users.id eq id }
                .map { ExposedUser(it[Users.name], it[Users.age]) }
                .singleOrNull()
        }

    suspend fun update(id: Int, user: ExposedUser) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[name] = user.name
                it[age] = user.age
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Users.deleteWhere { Users.id.eq(id) }
        }
    }
}

fun Routing.userRoutes(userService: UserService) {
    route("/users") {
        post {
            val user = call.receive<ExposedUser>()
            val id = userService.create(user)
            call.respond(id)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            when (val user = userService.read(id)) {
                null -> call.respond(HttpStatusCode.NotFound, "User not found")
                else -> call.respond(HttpStatusCode.OK, user)
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = call.receive<ExposedUser>()
            userService.update(id, user)
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            userService.delete(id)
            call.respond("User deleted successfully")
        }
    }
}

