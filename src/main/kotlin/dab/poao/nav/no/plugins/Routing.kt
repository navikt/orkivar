package dab.poao.nav.no.plugins

import dab.poao.nav.no.arkivering.arkiveringRoutes
import dab.poao.nav.no.database.JournalføringType
import dab.poao.nav.no.database.OppfølgingsperiodeId
import dab.poao.nav.no.database.JournalføringerRepository
import dab.poao.nav.no.dokark.DokarkClient
import dab.poao.nav.no.dokark.DokarkDistribusjonClient
import dab.poao.nav.no.health.healthEndpoints
import dab.poao.nav.no.pdfCaching.NyPdfSomSkalCaches
import dab.poao.nav.no.pdfCaching.PdfFraCache
import dab.poao.nav.no.pdfgenClient.PdfgenClient
import io.ktor.client.engine.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Application.configureRouting(
    httpClientEngine: HttpClientEngine,
    dokarkClient: DokarkClient = DokarkClient(environment.config, httpClientEngine),
    dokarkDistribusjonClient: DokarkDistribusjonClient = DokarkDistribusjonClient(environment.config, httpClientEngine),
    pdfgenClient: PdfgenClient = PdfgenClient(environment.config, httpClientEngine),
    cachePdf: (NyPdfSomSkalCaches) -> UUID,
    hentPdfFraCache: (UUID) -> PdfFraCache?,
    lagreJournalføring: suspend (JournalføringerRepository.NyJournalføring) -> Unit,
    hentJournalføringer: suspend (OppfølgingsperiodeId, JournalføringType) -> List<JournalføringerRepository.Journalfoering>
) {
    routing {
        healthEndpoints()
        get("/") {
            call.respondText("Hello World!")
        }
        authenticate("AzureAD", "TokenX") {
            arkiveringRoutes(dokarkClient, dokarkDistribusjonClient, pdfgenClient, lagreJournalføring, hentJournalføringer, cachePdf, hentPdfFraCache)
        }
    }
}
