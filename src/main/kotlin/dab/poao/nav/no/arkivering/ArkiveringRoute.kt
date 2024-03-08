package dab.poao.nav.no.arkivering

import dab.poao.nav.no.arkivering.dto.ArkiveringsPayload
import dab.poao.nav.no.arkivering.dto.ForhaandsvisningOutbound
import dab.poao.nav.no.azureAuth.logger
import dab.poao.nav.no.dokark.*
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
import java.util.UUID


fun Route.arkiveringRoutes(
    dokarkClient: DokarkClient,
    pdfgenClient: PdfgenClient,
    lagreJournalfoering: suspend (navIdent: String, fnr: Fnr, opprettet: LocalDateTime, referanse: UUID, journalpostId: String) -> Unit
) {
    post("/arkiver") {
        val token = call.hentUtBearerToken()
        val navIdent = call.hentNavIdentClaim()
        val arkiveringsPayload = call.arkiveringspayload()

        val tidspunkt = LocalDateTime.now()
        val pdfGenPayload = lagPdfgenPayload(arkiveringsPayload, tidspunkt)
        val referanse = UUID.randomUUID()

        val dokarkResult = runCatching {
            val pdfResult = pdfgenClient.generatePdf(
                payload = pdfGenPayload
            )
            when (pdfResult) {
                is PdfSuccess -> dokarkClient.opprettJournalpost(token, lagJournalpostData(pdfResult.pdfByteString, arkiveringsPayload, referanse, tidspunkt))
                is FailedPdfGen -> DokarkFail(pdfResult.message)
            }
        }
            .onFailure { logger.error("Noe uforventet", it) }
            .getOrElse { DokarkFail("Uventet feil") }

        when (dokarkResult) {
            is DokarkFail -> call.respond(HttpStatusCode.InternalServerError, dokarkResult.message)
            is DokarkSuccess -> {
                lagreJournalfoering(navIdent, arkiveringsPayload.metadata.fnr, tidspunkt, referanse, dokarkResult.journalpostId)
                call.respond("OK")
            }
        }
    }

    post("/forhaandsvisning") {
        val arkiveringsPayload = call.arkiveringspayload()
        val pdfgenPayload = lagPdfgenPayload(arkiveringsPayload, LocalDateTime.now())
        val pdfResult = pdfgenClient.generatePdf(pdfgenPayload)
        when (pdfResult) {
            is PdfSuccess -> call.respond(ForhaandsvisningOutbound(pdfResult.pdfByteString))
            is FailedPdfGen -> DokarkFail(pdfResult.message)
        }
    }
}

private fun ApplicationCall.hentUtBearerToken() =
    this.request.header("Authorization")
        ?.split(" ")
        ?.lastOrNull() ?: throw IllegalArgumentException("No token found")

private fun ApplicationCall.hentNavIdentClaim(): String {
    return authentication.principal<TokenValidationContextPrincipal>()?.context
        ?.getClaims("AzureAD")
        ?.getStringClaim("NAVident")
        ?: throw RuntimeException("Klarte ikke å hente NAVident claim fra tokenet")
}

private suspend fun ApplicationCall.arkiveringspayload(): ArkiveringsPayload {
    // Eksplisitt kasting av exception for å sikre at stacktrace kommer til loggen
    // Kan fjernes når feature er ferdig, og alt kan da gjøres inline der denne funksjonen brukes
    return try {
        this.receive<ArkiveringsPayload>()
    } catch (e: Exception) {
        logger.error("Feil ved deserialisering", e)
        throw e
    }
}

private fun lagPdfgenPayload(arkiveringsPayload: ArkiveringsPayload, tidspunkt: LocalDateTime): PdfgenPayload {
    val (fnr, navn, _, _, oppfølgingsperiodeStart, oppfølgingsperiodeSlutt) = arkiveringsPayload.metadata

    return PdfgenPayload(
        navn = navn,
        fnr = fnr,
        oppfølgingsperiodeStart = oppfølgingsperiodeStart,
        oppfølgingsperiodeSlutt = oppfølgingsperiodeSlutt,
        aktiviteter = arkiveringsPayload.aktiviteter,
        dialogtråder = arkiveringsPayload.dialogtråder,
        journalfoeringstidspunkt = tidspunkt.toString()
    )
}

private fun lagJournalpostData(pdf: ByteArray, arkiveringsPayload: ArkiveringsPayload, referanse: UUID, tidspunkt: LocalDateTime): JournalpostData {
    val (fnr, navn, sakId, fagsaksystem, oppfølgingsperiodeStart, oppfølgingsperiodeSlutt) = arkiveringsPayload.metadata

    return JournalpostData(
        pdf = pdf,
        navn = navn,
        fnr = fnr,
        tidspunkt = tidspunkt,
        sakId = sakId,
        fagsaksystem = fagsaksystem,
        eksternReferanse = referanse,
        oppfølgingsperiodeStart = oppfølgingsperiodeStart,
        oppfølgingsperiodeSlutt = oppfølgingsperiodeSlutt
    )
}
