package dab.poao.nav.no.arkivering

import dab.poao.nav.no.arkivering.dto.*
import dab.poao.nav.no.database.JournalføringType
import dab.poao.nav.no.database.OppfølgingsperiodeId
import dab.poao.nav.no.database.JournalføringerRepository
import dab.poao.nav.no.dokark.DokarkClient
import dab.poao.nav.no.dokark.DokarkDistribusjonClient
import dab.poao.nav.no.dokark.DokarkJournalpostFail
import dab.poao.nav.no.dokark.DokarkJournalpostResult
import dab.poao.nav.no.dokark.DokarkJournalpostSuccess
import dab.poao.nav.no.dokark.DokarkSendTilBrukerFail
import dab.poao.nav.no.dokark.DokarkSendTilBrukerSuccess
import dab.poao.nav.no.dokark.JournalpostData
import dab.poao.nav.no.dokark.JournalpostType
import dab.poao.nav.no.pdfCaching.NyPdfSomSkalCaches
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
    lagreJournalfoering: suspend (JournalføringerRepository.NyJournalføring) -> Unit,
    hentJournalføringer: suspend (OppfølgingsperiodeId, JournalføringType) -> List<JournalføringerRepository.Journalfoering>,
    cachePdf: (NyPdfSomSkalCaches) -> UUID,
) {
    suspend fun opprettJournalpost(journalføringspayload: JournalføringPayload, journalpostType: JournalpostType, token: String): DokarkJournalpostResult {

        val tidspunkt = LocalDateTime.now()
        val referanse = UUID.randomUUID()
        val pdfGenPayload = lagPdfgenPayload(journalføringspayload.pdfPayload, tidspunkt)

        val dokarkResult = runCatching {
            val pdfResult = pdfgenClient.generatePdf(
                payload = pdfGenPayload
            )
            when (pdfResult) {
                is PdfSuccess -> dokarkClient.opprettJournalpost(token, journalpostType,lagJournalpostData(pdfResult.pdfByteString, journalføringspayload, referanse, tidspunkt))
                is FailedPdfGen -> DokarkJournalpostFail(pdfResult.message)
            }
        }
            .onFailure { logger.error("Klarte ikke arkivere pdf: ${it.message}", it) }
            .getOrElse { DokarkJournalpostFail("Uventet feil") }

        return dokarkResult
    }

    suspend fun lagForhaandsvisning(payload: PdfData, type: JournalføringType, navIdent: String?): Result<ForhaandsvisningOutbound> {
        val pdfgenPayload = lagPdfgenPayload(payload, LocalDateTime.now())
        val pdfResult = pdfgenClient.generatePdf(pdfgenPayload)
        val oppfølgingsperiodeId = UUID.fromString(payload.oppfølgingsperiodeId)
        val sisteJournalføring = hentJournalføringer(oppfølgingsperiodeId, type).maxByOrNull { it.opprettetTidspunkt }
        return when (pdfResult) {
            is PdfSuccess -> {
                val uuidCachetPdf = if (navIdent != null) {
                    cachePdf(NyPdfSomSkalCaches(pdf = pdfResult.pdfByteString, fnr = payload.fnr, veilederIdent = navIdent))
                } else {
                    null
                }
                Result.success(ForhaandsvisningOutbound(pdfResult.pdfByteString, sisteJournalføring?.opprettetTidspunkt, uuidCachetPdf.toString()))
            }
            is FailedPdfGen -> {
                logger.error("Klarte ikke forhaandsvise pdf: ${pdfResult.message}")
                Result.failure(RuntimeException(pdfResult.message))
            }
        }
    }

    post("/arkiver") {
        val token = call.hentUtBearerToken()
        val navIdent = call.hentNavIdentClaim()
        val arkiveringsPayload = call.hentPayload<JournalføringPayload>()

        val dokarkResult = opprettJournalpost(arkiveringsPayload, JournalpostType.NOTAT, token)

        when (dokarkResult) {
            is DokarkJournalpostFail -> call.respond(HttpStatusCode.InternalServerError, dokarkResult.message)
            is DokarkJournalpostSuccess -> {
                lagreJournalfoering(
                    JournalføringerRepository.NyJournalføring(
                        navIdent = navIdent,
                        fnr = arkiveringsPayload.pdfPayload.fnr,
                        opprettetTidspunkt = dokarkResult.tidspunkt,
                        referanse = dokarkResult.referanse,
                        journalpostId = dokarkResult.journalpostId,
                        oppfølgingsperiodeId = UUID.fromString(arkiveringsPayload.pdfPayload.oppfølgingsperiodeId),
                        type = JournalføringType.JOURNALFØRING
                    )
                )
                call.respond(JournalføringOutbound(dokarkResult.tidspunkt.toKotlinLocalDateTime()))
            }
        }
    }

    post("/send-til-bruker") {
        val token = call.hentUtBearerToken()
        val navIdent = call.hentNavIdentClaim()
        val sendTilBrukerPayload = call.hentPayload<SendTilBrukerPayload>()
        val journalføringspayload = sendTilBrukerPayload.journalføringspayload
        val dokarkResult = opprettJournalpost(journalføringspayload, JournalpostType.UTGAAENDE, token)

        when (dokarkResult) {
            is DokarkJournalpostFail -> call.respond(HttpStatusCode.InternalServerError, dokarkResult.message)
            is DokarkJournalpostSuccess -> {
                lagreJournalfoering(
                    JournalføringerRepository.NyJournalføring(
                        navIdent = navIdent,
                        fnr = journalføringspayload.pdfPayload.fnr,
                        opprettetTidspunkt = dokarkResult.tidspunkt,
                        referanse = dokarkResult.referanse,
                        journalpostId = dokarkResult.journalpostId,
                        oppfølgingsperiodeId = UUID.fromString(journalføringspayload.pdfPayload.oppfølgingsperiodeId),
                        type = JournalføringType.SENDING_TIL_BRUKER
                    )
                )
                val sendTilBrukerResult = dokarkDistribusjonClient.sendJournalpostTilBruker(token, dokarkResult.journalpostId, journalføringspayload.fagsaksystem, sendTilBrukerPayload.brukerHarManuellOppfølging)
                when (sendTilBrukerResult) {
                    is DokarkSendTilBrukerFail -> call.respond(HttpStatusCode.InternalServerError)
                    is DokarkSendTilBrukerSuccess -> call.respond(JournalføringOutbound(dokarkResult.tidspunkt.toKotlinLocalDateTime()))
                }
            }
        }
    }

    post("/forhaandsvisning") {
        val forhåndsvisningPayload = call.hentPayload<PdfData>()
        val navIdentHvisInternBruker = call.hentNavIdentClaim()
        lagForhaandsvisning(forhåndsvisningPayload, JournalføringType.JOURNALFØRING, navIdentHvisInternBruker)
            .onSuccess { call.respond(it) }
            .onFailure { call.respond(HttpStatusCode.InternalServerError) }
    }

    post("/forhaandsvisning-send-til-bruker") {
        val forhåndsvisningPayload = call.hentPayload<PdfData>()
        val navIdentHvisInternBruker = call.hentNavIdentClaimOrNull()
        lagForhaandsvisning(forhåndsvisningPayload, JournalføringType.SENDING_TIL_BRUKER, navIdentHvisInternBruker)
            .onSuccess { call.respond(it) }
            .onFailure { call.respond(HttpStatusCode.InternalServerError) }
    }
}

private fun ApplicationCall.hentUtBearerToken() =
    this.request.header("Authorization")
        ?.split(" ")
        ?.lastOrNull() ?: throw IllegalAccessException("No bearer token found")

private fun ApplicationCall.hentNavIdentClaim(): String {
    return hentNavIdentClaimOrNull() ?: throw IllegalAccessException("Ingen NAVident-claim på tokenet")
}

private fun ApplicationCall.hentNavIdentClaimOrNull(): String? {
    return runCatching {
        authentication.principal<TokenValidationContextPrincipal>()?.context
            ?.getClaims("AzureAD")
            ?.getStringClaim("NAVident")
    }.getOrNull()
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
    val norskDatoFormat = DateTimeFormatter.ofPattern("d. MMMM uuuu", Locale.forLanguageTag("no"))

    return PdfgenPayload(
        navn = pdfData.navn,
        fnr = pdfData.fnr,
        tekstTilBruker = pdfData.tekstTilBruker,
        journalførendeEnhetNavn = pdfData.journalførendeEnhetNavn,
        brukteFiltre = pdfData.brukteFiltre.ifEmpty { null },
        oppfølgingsperiodeStart = pdfData.oppfølgingsperiodeStart,
        oppfølgingsperiodeSlutt = pdfData.oppfølgingsperiodeSlutt,
        aktiviteter = pdfData.aktiviteter,
        dialogtråder = pdfData.dialogtråder,
        mål = pdfData.mål,
        journalfoeringstidspunkt = tidspunkt.format(norskDatoKlokkeslettFormat),
        dagensDato = tidspunkt.format(norskDatoFormat)
    )
}

private fun lagJournalpostData(pdf: ByteArray, journalføringsPayload: JournalføringPayload, referanse: UUID, tidspunkt: LocalDateTime): JournalpostData {
    return JournalpostData(
        pdf = pdf,
        navn = journalføringsPayload.pdfPayload.navn,
        fnr = journalføringsPayload.pdfPayload.fnr,
        tidspunkt = tidspunkt,
        sakId = journalføringsPayload.sakId,
        fagsaksystem = journalføringsPayload.fagsaksystem,
        tema = journalføringsPayload.tema,
        eksternReferanse = referanse,
        oppfølgingsperiodeStart = journalføringsPayload.pdfPayload.oppfølgingsperiodeStart,
        oppfølgingsperiodeSlutt = journalføringsPayload.pdfPayload.oppfølgingsperiodeSlutt,
        journalførendeEnhet = journalføringsPayload.journalførendeEnhetId
    )
}
