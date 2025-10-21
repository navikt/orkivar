package dab.poao.nav.no.dokark

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.poao.dab.ktor_oauth_client.AzureClient
import no.nav.poao.dab.ktor_oauth_client.IncomingToken
import no.nav.poao.dab.ktor_oauth_client.OauthClientCredentialsConfig
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class DokarkClient(config: ApplicationConfig, httpClientEngine: HttpClientEngine) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    val clientScope = config.property("dokark.client-scope").getString()
    val clientUrl = config.property("dokark.client-url").getString()
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

    suspend fun opprettJournalpost(token: IncomingToken, journalpostData: JournalpostData): DokarkResult {
        val res = runCatching {  client.post("$clientUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true") {
            header("authorization", "Bearer ${azureClient.getOnBehalfOfToken("openid profile $clientScope", token)}")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(lagJournalpost(journalpostData)))
        } }
            .onFailure { logger.error("Noe gikk galt", it) }
            .getOrElse { return DokarkFail("Kunne ikke poste til joark") }
        if (!res.status.isSuccess()) {
            logger.warn("Feilet å opprette journalpost: HTTP ${res.status.value} - ", res.bodyAsText())
            return DokarkFail("Feilet å laste opp til joark")
        }

        val dokarkresponse = res.body<DokarkResponse>()
        if (!dokarkresponse.journalpostferdigstilt) {
            logger.warn("Opprettet journalpost, men den kunne ikke bli ferdigstilt automatisk")
        }
        return DokarkSuccess(journalpostId = dokarkresponse.journalpostId)
    }
}

fun ApplicationConfig.toOauthConfig(): OauthClientCredentialsConfig {
    val azureClientId = this.property("azure.client-id").getString()
    val clientSecret = this.property("azure.client-secret").getString()
    val tokenEndpoint = this.property("azure.token-endpoint").getString()
    return OauthClientCredentialsConfig(
        clientId = azureClientId,
        clientSecret = clientSecret,
        tokenEndpoint = tokenEndpoint
    )
}

data class JournalpostData(
    val pdf: ByteArray,
    val navn: String,
    val fnr: String,
    val tidspunkt: LocalDateTime,
    val sakId: Long,
    val fagsaksystem: String,
    val tema: String,
    val eksternReferanse: UUID,
    val oppfølgingsperiodeStart: String,
    val oppfølgingsperiodeSlutt: String?,
    val journalførendeEnhet: String,
)

@Serializable
data class DokarkResponse(
    val journalpostId: String,
    val journalstatus: String,
    val melding: String?,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokarkResponseDokument>
)

@Serializable
data class DokarkResponseDokument(
    val dokumentInfoId: String
)

sealed interface DokarkResult
data class DokarkSuccess(val journalpostId: String): DokarkResult
data class DokarkFail(val message: String): DokarkResult