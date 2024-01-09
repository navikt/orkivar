package dab.poao.nav.no.plugins

import dab.poao.nav.no.health.healthEndpoints
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        healthEndpoints()
        get("/") {
            call.respondText("Hello World!")
        }
    }
}
