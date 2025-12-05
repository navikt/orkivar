package dab.poao.nav.no.arkivering

import dab.poao.nav.no.arkivering.dto.*
import dab.poao.nav.no.database.OppfølgingsperiodeId
import dab.poao.nav.no.database.Repository
import dab.poao.nav.no.dokark.DokarkClient
import dab.poao.nav.no.dokark.DokarkDistribusjonClient
import dab.poao.nav.no.dokark.DokarkJournalpostFail
import dab.poao.nav.no.dokark.DokarkJournalpostResult
import dab.poao.nav.no.dokark.DokarkJournalpostSuccess
import dab.poao.nav.no.dokark.DokarkSendTilBrukerFail
import dab.poao.nav.no.dokark.DokarkSendTilBrukerSuccess
import dab.poao.nav.no.dokark.JournalpostData
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
import kotlinx.datetime.toKotlinLocalDateTime
import no.nav.security.token.support.v3.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val logger = LoggerFactory.getLogger("ArkiveringRoutes.kt")

fun Route.arkiveringRoutes(
    dokarkClient: DokarkClient,
    dokarkDistribusjonClient: DokarkDistribusjonClient,
    pdfgenClient: PdfgenClient,
    lagreJournalfoering: suspend (Repository.NyJournalføring) -> Unit,
    hentJournalføringer: suspend (OppfølgingsperiodeId) -> List<Repository.Journalfoering>
) {
    suspend fun opprettJournalpost(arkiveringsPayload: JournalføringPayload, token: String): DokarkJournalpostResult {

        val tidspunkt = LocalDateTime.now()
        val referanse = UUID.randomUUID()
        val pdfGenPayload = lagPdfgenPayload(arkiveringsPayload, tidspunkt)

        val dokarkResult = runCatching {
            val pdfResult = pdfgenClient.generatePdf(
                payload = pdfGenPayload
            )
            when (pdfResult) {
                is PdfSuccess -> dokarkClient.opprettJournalpost(token, lagJournalpostData(pdfResult.pdfByteString, arkiveringsPayload, referanse, tidspunkt))
                is FailedPdfGen -> DokarkJournalpostFail(pdfResult.message)
            }
        }
            .onFailure { logger.error("Klarte ikke arkivere pdf: ${it.message}", it) }
            .getOrElse { DokarkJournalpostFail("Uventet feil") }

        return dokarkResult
    }

    post("/arkiver") {
        val token = call.hentUtBearerToken()
        val navIdent = call.hentNavIdentClaim()
        val arkiveringsPayload = call.hentPayload<JournalføringPayload>()

        val dokarkResult = opprettJournalpost(arkiveringsPayload, token)

        when (dokarkResult) {
            is DokarkJournalpostFail -> call.respond(HttpStatusCode.InternalServerError, dokarkResult.message)
            is DokarkJournalpostSuccess -> {
                lagreJournalfoering(
                    Repository.NyJournalføring(
                        navIdent = navIdent,
                        fnr = arkiveringsPayload.fnr,
                        opprettetTidspunkt = dokarkResult.tidspunkt,
                        referanse = dokarkResult.referanse,
                        journalpostId = dokarkResult.journalpostId,
                        oppfølgingsperiodeId = UUID.fromString(arkiveringsPayload.oppfølgingsperiodeId)
                    )
                )
                call.respond(JournalføringOutbound(dokarkResult.tidspunkt.toKotlinLocalDateTime()))
            }
        }
    }

    post("/send-til-bruker") {
        val token = call.hentUtBearerToken()
        val navIdent = call.hentNavIdentClaim()
        val arkiveringsPayload = call.hentPayload<JournalføringPayload>()
        val dokarkResult = opprettJournalpost(arkiveringsPayload, token)

        when (dokarkResult) {
            is DokarkJournalpostFail -> call.respond(HttpStatusCode.InternalServerError, dokarkResult.message)
            is DokarkJournalpostSuccess -> {
                lagreJournalfoering(
                    Repository.NyJournalføring(
                        navIdent = navIdent,
                        fnr = arkiveringsPayload.fnr,
                        opprettetTidspunkt = dokarkResult.tidspunkt,
                        referanse = dokarkResult.referanse,
                        journalpostId = dokarkResult.journalpostId,
                        oppfølgingsperiodeId = UUID.fromString(arkiveringsPayload.oppfølgingsperiodeId)
                    )
                )
                val sendTilBrukerResult = dokarkDistribusjonClient.sendJournalpostTilBruker(token, dokarkResult.journalpostId, arkiveringsPayload.fagsaksystem)
                when (sendTilBrukerResult) {
                    is DokarkSendTilBrukerFail -> call.respond(HttpStatusCode.InternalServerError)
                    is DokarkSendTilBrukerSuccess -> call.respond(JournalføringOutbound(dokarkResult.tidspunkt.toKotlinLocalDateTime()))
                }
            }
        }
    }

    post("/forhaandsvisning") {
        val forhåndsvisningPayload = call.hentPayload<ForhåndsvisningPayload>()
        val pdfgenPayload = lagPdfgenPayload(forhåndsvisningPayload, LocalDateTime.now())
        val pdfResult = pdfgenClient.generatePdf(pdfgenPayload)
        val oppfølgingsperiodeId = UUID.fromString(forhåndsvisningPayload.oppfølgingsperiodeId)
        val sisteJournalføring = hentJournalføringer(oppfølgingsperiodeId).sortedByDescending { it.opprettetTidspunkt }.firstOrNull()
        when (pdfResult) {
            is PdfSuccess -> call.respond(ForhaandsvisningOutbound(pdfResult.pdfByteString, sisteJournalføring?.opprettetTidspunkt))
            is FailedPdfGen -> {
                logger.error("Klarte ikke forhaandsvise pdf: ${pdfResult.message}")
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}


private fun ApplicationCall.hentUtBearerToken() =
    this.request.header("Authorization")
        ?.split(" ")
        ?.lastOrNull() ?: throw IllegalAccessException("No bearer token found")

private fun ApplicationCall.hentNavIdentClaim(): String {
    return authentication.principal<TokenValidationContextPrincipal>()?.context
        ?.getClaims("AzureAD")
        ?.getStringClaim("NAVident")
        ?: throw IllegalAccessException("Ingen NAVident-claim på tokenet")
}

private suspend inline fun <reified T: Any> ApplicationCall.hentPayload(): T {
    // Eksplisitt kasting av exception for å sikre at stacktrace kommer til loggen
    // Kan fjernes når feature er ferdig, og alt kan da gjøres inline der denne funksjonen brukes
    return try {
        this.receive()
    } catch (e: Exception) {
        logger.error("Feil ved deserialisering", e)
        throw e
    }
}

private fun lagPdfgenPayload(pdfData: PdfData, tidspunkt: LocalDateTime): PdfgenPayload {
    val norskDatoKlokkeslettFormat = DateTimeFormatter.ofPattern("d. MMMM uuuu 'kl.' HH.mm", Locale.forLanguageTag("no"))

    val formatertTidspunkt = tidspunkt.format(norskDatoKlokkeslettFormat)

    return PdfgenPayload(
        navn = pdfData.navn,
        fnr = pdfData.fnr,
        oppfølgingsperiodeStart = pdfData.oppfølgingsperiodeStart,
        oppfølgingsperiodeSlutt = pdfData.oppfølgingsperiodeSlutt,
        aktiviteter = pdfData.aktiviteter,
        dialogtråder = pdfData.dialogtråder,
        mål = pdfData.mål,
        journalfoeringstidspunkt = formatertTidspunkt
    )
}

private fun lagJournalpostData(pdf: ByteArray, journalføringsPayload: JournalføringPayload, referanse: UUID, tidspunkt: LocalDateTime): JournalpostData {
    return JournalpostData(
        pdf = pdf,
        navn = journalføringsPayload.navn,
        fnr = journalføringsPayload.fnr,
        tidspunkt = tidspunkt,
        sakId = journalføringsPayload.sakId,
        fagsaksystem = journalføringsPayload.fagsaksystem,
        tema = journalføringsPayload.tema,
        eksternReferanse = referanse,
        oppfølgingsperiodeStart = journalføringsPayload.oppfølgingsperiodeStart,
        oppfølgingsperiodeSlutt = journalføringsPayload.oppfølgingsperiodeSlutt,
        journalførendeEnhet = journalføringsPayload.journalførendeEnhet
    )
}
