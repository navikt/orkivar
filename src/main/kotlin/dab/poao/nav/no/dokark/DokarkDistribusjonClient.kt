package dab.poao.nav.no.dokark

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.poao.dab.ktor_oauth_client.AzureClient
import no.nav.poao.dab.ktor_oauth_client.IncomingToken
import org.slf4j.LoggerFactory

class DokarkDistribusjonClient(config: ApplicationConfig, httpClientEngine: HttpClientEngine) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    val clientScope = config.property("dokark.distribusjon.client-scope").getString()
    val clientUrl = config.property("dokark.distribusjon.client-url").getString()
    val azureClient = AzureClient(config.toOauthConfig())
    val client = HttpClient(httpClientEngine) {
        install(ContentNegotiation) {
            json()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    suspend fun sendJournalpostTilBruker(
        token: IncomingToken,
        journalpostId: String,
        fagsaksystem: String,
        tvingPrint: Boolean,
    ): DokarkSendTilBrukerResult {
        val url = "$clientUrl/rest/v1/distribuerjournalpost"
        val res = runCatching {
            client.post(url) {
                header(
                    "authorization",
                    "Bearer ${azureClient.getOnBehalfOfToken("openid profile $clientScope", token)}"
                )
                contentType(ContentType.Application.Json)
                setBody(
                    Json.encodeToString(
                        DistribuerJournalpost(
                            journalpostId = journalpostId,
                            bestillendeFagsystem = fagsaksystem,
                            dokumentProdApp = "orkivar",
                            distribusjonstype = "ANNET",
                            tvingKanal = if (tvingPrint) "PRINT" else null,
                            distribusjonstidspunkt = "UMIDDELBART"
                        )
                    )
                )
            }
        }
            .onFailure { logger.error("Noe gikk galt", it) }
            .getOrElse { return DokarkSendTilBrukerFail() }
        if (!res.status.isSuccess()) {
            logger.warn("Feilet å distribuere journalpost: HTTP ${res.status.value} - URL: $url")
            return DokarkSendTilBrukerFail()
        }

        val dokarkresponse = res.body<DistribuerJournalpostResponse>()
        return DokarkSendTilBrukerSuccess(dokarkresponse.bestillingsId)
    }
}

@Serializable
data class DistribuerJournalpostResponse(
    val bestillingsId: String
)

sealed interface DokarkSendTilBrukerResult
class DokarkSendTilBrukerSuccess(val bestillingsId: String) : DokarkSendTilBrukerResult
class DokarkSendTilBrukerFail : DokarkSendTilBrukerResult