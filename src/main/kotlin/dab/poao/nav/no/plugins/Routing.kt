package dab.poao.nav.no.plugins

import dab.poao.nav.no.dokark.DokarkClient
import dab.poao.nav.no.dokark.DokarkFail
import dab.poao.nav.no.dokark.DokarkResult
import dab.poao.nav.no.dokark.DokarkSuccess
import dab.poao.nav.no.health.healthEndpoints
import dab.poao.nav.no.pdfgenClient.FailedPdfGen
import dab.poao.nav.no.pdfgenClient.PdfSuccess
import dab.poao.nav.no.pdfgenClient.PdfgenClient
import dab.poao.nav.no.pdfgenClient.PdfgenResult
import dab.poao.nav.no.pdfgenClient.dto.PdfgenPayload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.lang.IllegalArgumentException
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
            post("/arkiver") {
                val token = call.request.header("Authorization")
                    ?.split(" ")
                    ?.lastOrNull() ?: throw IllegalArgumentException("No token found")

                val navn = "Navn navnesen"
                val fnr = "11837798592"
                val timestamp = ZonedDateTime.now().toString()

                val pdfResult = pdfgenClient.generatePdf(payload = PdfgenPayload(navn, fnr, timestamp))

                val dokarkResult = when (pdfResult) {
                    is PdfSuccess -> dokarkClient.opprettJournalpost(token, pdfResult, navn, fnr)
                    is FailedPdfGen -> DokarkFail(pdfResult.message)
                }
                when (dokarkResult) {
                    is DokarkFail -> call.respond(HttpStatusCode.InternalServerError, dokarkResult.message)
                    is DokarkSuccess -> call.respond("OK")
                }
            }
        }
    }
}
