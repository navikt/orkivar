package dab.poao.nav.no.plugins

import dab.poao.nav.no.arkivering.arkiveringRoutes
import dab.poao.nav.no.dokark.DokarkClient
import dab.poao.nav.no.health.healthEndpoints
import dab.poao.nav.no.pdfgenClient.PdfgenClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.ZonedDateTime

fun Application.configureRouting(
    dokarkClient: DokarkClient = DokarkClient(environment.config),
    pdfgenClient: PdfgenClient = PdfgenClient(environment.config)
) {
    routing {
        healthEndpoints()
        get("/") {
            call.respondText("Hello World!")
        }
        authenticate("AzureAD") {
            arkiveringRoutes(dokarkClient, pdfgenClient)
        }
    }
}
