package dab.poao.nav.no.plugins

import dab.poao.nav.no.arkivering.arkiveringRoutes
import dab.poao.nav.no.database.Repository
import dab.poao.nav.no.dokark.DokarkClient
import dab.poao.nav.no.dokark.Fnr
import dab.poao.nav.no.health.healthEndpoints
import dab.poao.nav.no.pdfgenClient.PdfgenClient
import io.ktor.client.engine.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.util.*

fun Application.configureRouting(
    httpClientEngine: HttpClientEngine,
    dokarkClient: DokarkClient = DokarkClient(environment.config, httpClientEngine),
    pdfgenClient: PdfgenClient = PdfgenClient(environment.config, httpClientEngine),
    lagreJournalfoering: suspend (Repository.NyJournalføring) -> Unit
) {
    routing {
        healthEndpoints()
        get("/") {
            call.respondText("Hello World!")
        }
        authenticate("AzureAD") {
            arkiveringRoutes(dokarkClient, pdfgenClient, lagreJournalfoering)
        }
    }
}
