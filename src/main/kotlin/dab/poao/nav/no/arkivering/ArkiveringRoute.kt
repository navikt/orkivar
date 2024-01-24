package dab.poao.nav.no.arkivering

import dab.poao.nav.no.arkivering.dto.ArkiveringsPayload
import dab.poao.nav.no.azureAuth.logger
import dab.poao.nav.no.dokark.DokarkClient
import dab.poao.nav.no.dokark.DokarkFail
import dab.poao.nav.no.dokark.DokarkSuccess
import dab.poao.nav.no.dokark.Fnr
import dab.poao.nav.no.pdfgenClient.FailedPdfGen
import dab.poao.nav.no.pdfgenClient.PdfSuccess
import dab.poao.nav.no.pdfgenClient.PdfgenClient
import dab.poao.nav.no.pdfgenClient.dto.PdfgenPayload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.lang.IllegalArgumentException
import java.time.LocalDateTime


fun Route.arkiveringRoutes(
    dokarkClient: DokarkClient,
    pdfgenClient: PdfgenClient,
    lagreJournalfoering: suspend (navIdent: String, fnr: Fnr, opprettet: LocalDateTime) -> Unit
) {
    fun ApplicationCall.getClaim(name: String): String? {
        return authentication.principal<TokenValidationContextPrincipal>()?.context
            ?.getClaims("AzureAD")
            ?.getStringClaim("NAVident")
    }

    post("/arkiver") {
        val token = call.request.header("Authorization")
            ?.split(" ")
            ?.lastOrNull() ?: throw IllegalArgumentException("No token found")

        val (metadata) = call.receive<ArkiveringsPayload>()
        val (fnr, navn) = metadata
        val tidspunkt = LocalDateTime.now()
        val navIdent = call.getClaim("NAVident") ?: throw RuntimeException("Klarte ikke å hente NAVident claim fra tokenet")

        val dokarkResult = runCatching {
            val pdfResult = pdfgenClient.generatePdf(payload = PdfgenPayload(navn, fnr, tidspunkt.toString()))
            when (pdfResult) {
                is PdfSuccess -> dokarkClient.opprettJournalpost(token, pdfResult, navn, fnr)
                is FailedPdfGen -> DokarkFail(pdfResult.message)
            }
        }
            .onFailure { logger.error("Noe uforventet", it) }
            .getOrElse { DokarkFail("Uventet feil") }
        when (dokarkResult) {
            is DokarkFail -> call.respond(HttpStatusCode.InternalServerError, dokarkResult.message)
            is DokarkSuccess -> {
                lagreJournalfoering(navIdent, fnr, tidspunkt)
                call.respond("OK")
            }
        }
    }
}
