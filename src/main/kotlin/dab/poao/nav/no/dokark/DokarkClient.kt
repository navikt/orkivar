package dab.poao.nav.no.dokark

import dab.poao.nav.no.azureAuth.logger
import dab.poao.nav.no.pdfgenClient.PdfSuccess
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import no.nav.poao.dab.ktor_oauth_client.AzureClient
import no.nav.poao.dab.ktor_oauth_client.IncomingToken
import no.nav.poao.dab.ktor_oauth_client.OauthClientCredentialsConfig
import java.time.LocalDateTime

class DokarkClient(config: ApplicationConfig, httpClientEngine: HttpClientEngine) {
    val clientScope = config.property("dokark.client-scope").getString()
    val clientUrl = config.property("dokark.client-url").getString()
    val azureClient = AzureClient(config.toOauthConfig())
    val client = HttpClient(httpClientEngine) {
        install(ContentNegotiation) {
            json()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.BODY // TODO: Fjern etter at debugging er ferdig
        }
    }

    suspend fun opprettJournalpost(token: IncomingToken, pdfResult: PdfSuccess, navn: String, fnr: String, tidspunkt: LocalDateTime): DokarkResult {
        val res = runCatching {  client.post("$clientUrl/rest/journalpostapi/v1/journalpost") {
            header("authorization", "Bearer ${azureClient.getOnBehalfOfToken("openid profile $clientScope", token)}")
            contentType(ContentType.Application.Json)
            setBody(lagJournalpost(pdfResult.pdfByteString, navn, fnr, tidspunkt))
        } }
            .onFailure { logger.error("Noe gikk galt", it) }
            .getOrElse { return DokarkFail("Kunne ikke poste til joark") }
        if (!res.status.isSuccess()) {
            logger.warn("Feilet å opprette journalpost:", res.bodyAsText())
            return DokarkFail("Feilet å laste opp til joark")
        }
        return DokarkSuccess
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

sealed interface DokarkResult
data object DokarkSuccess: DokarkResult
data class DokarkFail(val message: String): DokarkResult