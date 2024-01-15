package dab.poao.nav.no.arkivering

import dab.poao.nav.no.arkivering.dto.ArkiveringsPayload
import dab.poao.nav.no.dokark.DokarkClient
import dab.poao.nav.no.dokark.DokarkFail
import dab.poao.nav.no.dokark.DokarkSuccess
import dab.poao.nav.no.pdfgenClient.FailedPdfGen
import dab.poao.nav.no.pdfgenClient.PdfSuccess
import dab.poao.nav.no.pdfgenClient.PdfgenClient
import dab.poao.nav.no.pdfgenClient.dto.PdfgenPayload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.lang.IllegalArgumentException
import java.time.ZonedDateTime

fun Route.arkiveringRoutes(
    dokarkClient: DokarkClient,
    pdfgenClient: PdfgenClient
) {
    post("/arkiver") {
        val token = call.request.header("Authorization")
            ?.split(" ")
            ?.lastOrNull() ?: throw IllegalArgumentException("No token found")

        val (metadata) = call.receive<ArkiveringsPayload>()
        val (fnr, navn, tidspunkt) = metadata

        val pdfResult = pdfgenClient.generatePdf(payload = PdfgenPayload(metadata.navn, fnr, tidspunkt))

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