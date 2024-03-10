package com.example.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            call.respondHtml {
                layout {
                    homeScreen()
                }
            }
        }
        staticResources("/static", "static")
    }
}

fun HTML.layout(content: BODY.() -> Unit) {
    head {
        title { +"Ktor: Getting Started" }
        script {
//            src = "/static/uno.global.js"
            src = "https://cdn.jsdelivr.net/npm/@unocss/runtime/uno.global.js"
        }
        script {
//            src = "/static/htmx.js"
            src = "https://unpkg.com/htmx.org@1.9.10"
        }
        link {
            rel = "stylesheet"
//            href = "/static/modern-normalize.css"
            href = "https://esm.sh/modern-normalize@2.0.0/modern-normalize.css"
        }
    }
    body {
        div {
            id = "menu"
            navbar(links)
        }
        div {
            id = "content"
            this@body.content()
        }
    }
}

data class NavLinkData(val title: String, val url: String)

val links = listOf(
    NavLinkData(title = "Home", url = "/"),
    NavLinkData(title = "Static", url = "/static")
)

fun FlowContent.navbar(links: List<NavLinkData>) {
    nav {
        classes = setOf("bg-gray-800", "text-white", "p-2")

        ul {
            links.forEach {
                li {
                    a(href = it.url) { +it.title }
                }
            }
        }
    }
}

fun FlowContent.homeScreen() {
    h1 { +"Goal Tracker Demo" }

    goalCreationForm()

    output {
        attributes["hx-get"] = "/view/goal-list"
        attributes["hx-swap"] = "innerHTML"
        attributes["hx-trigger"] = "load"
    }
}

