package dab.poao.nav.no.arkivering

import dab.poao.nav.no.arkivering.dto.*
import dab.poao.nav.no.arkivering.dto.Metadata
import dab.poao.nav.no.azureAuth.logger
import dab.poao.nav.no.database.OppfølgingsperiodeId
import dab.poao.nav.no.database.Repository
import dab.poao.nav.no.dokark.DokarkClient
import dab.poao.nav.no.dokark.DokarkFail
import dab.poao.nav.no.dokark.DokarkSuccess
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
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


fun Route.arkiveringRoutes(
    dokarkClient: DokarkClient,
    pdfgenClient: PdfgenClient,
    lagreJournalfoering: suspend (Repository.NyJournalføring) -> Unit,
    hentJournalføringer: suspend (OppfølgingsperiodeId) -> List<Repository.Journalfoering>
) {
    post("/arkiver") {
        val token = call.hentUtBearerToken()
        val navIdent = call.hentNavIdentClaim()
        val arkiveringsPayload = call.hentPayload<JournalføringsPayload>()

        val tidspunkt = LocalDateTime.now()
        val pdfGenPayload =
            lagPdfgenPayload(arkiveringsPayload.metadata, arkiveringsPayload.aktivitetsplanInnhold, tidspunkt)
        val referanse = UUID.randomUUID()

        val dokarkResult = runCatching {
            val pdfResult = pdfgenClient.generatePdf(
                payload = pdfGenPayload
            )
            when (pdfResult) {
                is PdfSuccess -> dokarkClient.opprettJournalpost(
                    token,
                    lagJournalpostData(
                        pdfResult.pdfByteString,
                        arkiveringsPayload.metadata,
                        arkiveringsPayload.journalføringsMetadata,
                        referanse,
                        tidspunkt
                    )
                )

                is FailedPdfGen -> DokarkFail(pdfResult.message)
            }
        }
            .onFailure { logger.error("Noe uforventet", it) }
            .getOrElse { DokarkFail("Uventet feil") }

        when (dokarkResult) {
            is DokarkFail -> call.respond(HttpStatusCode.InternalServerError, dokarkResult.message)
            is DokarkSuccess -> {
                lagreJournalfoering(
                    Repository.NyJournalføring(
                        navIdent = navIdent,
                        fnr = arkiveringsPayload.metadata.fnr,
                        opprettetTidspunkt = tidspunkt,
                        referanse = referanse,
                        journalpostId = dokarkResult.journalpostId,
                        oppfølgingsperiodeId = UUID.fromString(arkiveringsPayload.metadata.oppfølgingsperiodeId)
                    )
                )
                call.respond(JournalføringOutbound(tidspunkt.toKotlinLocalDateTime()))
            }
        }
    }

    post("/forhaandsvisning") {
        val forhåndsvisningsPayload = call.hentPayload<ForhåndsvisningPayload>()
        val pdfgenPayload = lagPdfgenPayload(
            forhåndsvisningsPayload.metadata,
            forhåndsvisningsPayload.aktivitetsplanInnhold,
            LocalDateTime.now()
        )
        val pdfResult = pdfgenClient.generatePdf(pdfgenPayload)
        val oppfølgingsperiodeId = UUID.fromString(forhåndsvisningsPayload.metadata.oppfølgingsperiodeId)
        val sisteJournalføring =
            hentJournalføringer(oppfølgingsperiodeId).sortedByDescending { it.opprettetTidspunkt }.firstOrNull()
        when (pdfResult) {
            is PdfSuccess -> call.respond(
                ForhaandsvisningOutbound(
                    pdfResult.pdfByteString,
                    sisteJournalføring?.opprettetTidspunkt
                )
            )

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

private suspend inline fun <reified T : Any> ApplicationCall.hentPayload(): T {
    // Eksplisitt kasting av exception for å sikre at stacktrace kommer til loggen
    // Kan fjernes når feature er ferdig, og alt kan da gjøres inline der denne funksjonen brukes
    return try {
        this.receive<T>()
    } catch (e: Exception) {
        logger.error("Feil ved deserialisering", e)
        throw e
    }
}

private fun lagPdfgenPayload(
    metadata: Metadata,
    aktivitetsplanInnhold: AktivitetsplanInnhold,
    tidspunkt: LocalDateTime
): PdfgenPayload {
    val norskDatoKlokkeslettFormat =
        DateTimeFormatter.ofPattern("d. MMMM uuuu 'kl.' HH:mm", Locale.forLanguageTag("no"))
    val formatertTidspunkt = tidspunkt.format(norskDatoKlokkeslettFormat)

    return PdfgenPayload(
        navn = metadata.navn,
        fnr = metadata.fnr,
        oppfølgingsperiodeStart = metadata.oppfølgingsperiodeStart,
        oppfølgingsperiodeSlutt = metadata.oppfølgingsperiodeSlutt,
        aktiviteter = aktivitetsplanInnhold.aktiviteter,
        dialogtråder = aktivitetsplanInnhold.dialogtråder,
        mål = aktivitetsplanInnhold.mål,
        journalfoeringstidspunkt = formatertTidspunkt
    )
}

private fun lagJournalpostData(
    pdf: ByteArray,
    metadata: Metadata,
    journalføringsMetadata: JournalføringsMetadata,
    referanse: UUID,
    tidspunkt: LocalDateTime
): JournalpostData {

    return JournalpostData(
        pdf = pdf,
        navn = metadata.navn,
        fnr = metadata.fnr,
        tidspunkt = tidspunkt,
        sakId = journalføringsMetadata.sakId,
        fagsaksystem = journalføringsMetadata.fagsaksystem,
        eksternReferanse = referanse,
        oppfølgingsperiodeStart = metadata.oppfølgingsperiodeStart,
        oppfølgingsperiodeSlutt = metadata.oppfølgingsperiodeSlutt,
        journalførendeEnhet = journalføringsMetadata.journalførendeEnhet
    )
}
