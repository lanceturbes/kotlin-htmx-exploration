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
data class GoalOptions(val title: String)

@Serializable
data class Goal(val id: Int, val title: String, val isComplete: Boolean)

class GoalService(database: Database) {
    object GoalTable : Table() {
        val id = integer("id").autoIncrement()
        val title = text("title")
        val isComplete = bool("is_complete")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(GoalTable)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(goal: GoalOptions): Int =
        dbQuery {
            GoalTable.insert {
                it[title] = goal.title
                it[isComplete] = false
            }[GoalTable.id]
        }

    suspend fun read(id: Int): Goal? =
        dbQuery {
            GoalTable.select { GoalTable.id eq id }
                .map {
                    Goal(
                        it[GoalTable.id],
                        it[GoalTable.title],
                        it[GoalTable.isComplete]
                    )
                }
                .singleOrNull()
        }

    suspend fun readAll(): List<Goal> =
        dbQuery {
            GoalTable.selectAll()
                .map {
                    Goal(
                        it[GoalTable.id],
                        it[GoalTable.title],
                        it[GoalTable.isComplete]
                    )
                }
        }

    suspend fun update(id: Int, goal: GoalOptions) {
        dbQuery {
            GoalTable.update({ GoalTable.id eq id }) {
                it[title] = goal.title
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            GoalTable.deleteWhere { GoalTable.id eq id }
        }
    }

    suspend fun toggleComplete(id: Int, isNextComplete: Boolean) {
        dbQuery {
            GoalTable.update({ GoalTable.id eq id }) {
                it[isComplete] = isNextComplete
            }
        }
    }
}

fun Routing.goalRoutes(goalService: GoalService) {
    route("/goals") {
        get {
            val goals = goalService.readAll()
            call.respond(HttpStatusCode.OK, goals)
        }

        post {
            val goal = call.receive<GoalOptions>()
            val id = goalService.create(goal)
            call.respond(HttpStatusCode.Created, id)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            when (val goal = goalService.read(id)) {
                null -> call.respond(HttpStatusCode.NotFound, "Goal not found")
                else -> call.respond(HttpStatusCode.OK, goal)
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val goal = call.receive<GoalOptions>()
            goalService.update(id, goal)
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            goalService.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
