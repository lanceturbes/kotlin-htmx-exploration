package com.example.plugins

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.html.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
    val userService = UserService(database)
    val goalService = GoalService(database)

    routing {
        goalRoutes(goalService)
        userRoutes(userService)

        route("/view") {
            get("/goal-list") {
                val goals = goalService.readAll()

                call.respondHtml {
                    body {
                        goalList(goals)
                    }
                }
            }

            post("/goal") {
                val title = call.receiveParameters()["title"] ?: ""
                goalService.create(GoalOptions(title))

                val goals = goalService.readAll()
                call.respondHtml {
                    body {
                        goalList(goals)
                    }
                }
            }

            patch("/goal/{id}/mark-complete") {
                val id = call.parameters["id"]?.toInt() ?: 0
                goalService.toggleComplete(id, true)

                val goals = goalService.readAll()
                call.respondHtml {
                    body {
                        goalList(goals)
                    }
                }
            }

            patch("/goal/{id}/mark-incomplete") {
                val id = call.parameters["id"]?.toInt() ?: 0
                goalService.toggleComplete(id, false)

                val goals = goalService.readAll()
                call.respondHtml {
                    body {
                        goalList(goals)
                    }
                }
            }

            delete("/goal/{id}") {
                val id = call.parameters["id"]?.toInt() ?: 0
                goalService.delete(id)
                val goals = goalService.readAll()
                call.respondHtml {
                    body {
                        goalList(goals)
                    }
                }
            }
        }
    }
}

fun FlowContent.goalList(goals: List<Goal>) {
    ul {
        id = "goal-list"

        if (goals.isEmpty()) {
            li { +"No goals found" }
        } else {
            goals.forEach {
                li {
                    +"${it.title} - ${if (it.isComplete) "Complete" else "Incomplete"}"
                    button {
                        attributes["hx-patch"] =
                            "/view/goal/${it.id}/${if (it.isComplete) "mark-incomplete" else "mark-complete"}"
                        attributes["hx-swap"] = "outerHTML"
                        attributes["hx-target"] = "#goal-list"
                        +"Toggle Completion"
                    }
                    button {
                        attributes["hx-delete"] = "/view/goal/${it.id}"
                        attributes["hx-swap"] = "outerHTML"
                        attributes["hx-target"] = "#goal-list"
                        +"Delete"
                    }
                }
            }
        }
    }
}

fun FlowContent.goalCreationForm() {
    form {
        attributes["hx-post"] = "/view/goal"
        attributes["hx-swap"] = "outerHTML"
        attributes["hx-target"] = "#goal-list"

        input {
            name = "title"
            type = InputType.text
            placeholder = "Goal title"
        }
        button { +"Create" }
    }
}