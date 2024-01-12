package dab.poao.nav.no.plugins

import dab.poao.nav.no.dokark.DokarkClient
import dab.poao.nav.no.health.healthEndpoints
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.lang.IllegalArgumentException

fun Application.configureRouting(dokarkClient: DokarkClient = DokarkClient(environment.config)) {
    routing {
        healthEndpoints()
        get("/") {
            call.respondText("Hello World!")
        }
        authenticate("AzureAD") {
            post("/arkiver") {
                call.request.header("Authorization")
                    ?.split(" ")
                    ?.lastOrNull()
//                call.principal<TokenValidationContextPrincipal>()
//                    ?.context?.firstValidToken?.encodedToken
                    ?.let { dokarkClient.opprettJournalpost(it) }
                    ?: throw IllegalArgumentException("No token found")
                call.respond("OK")
            }
        }
    }
}
