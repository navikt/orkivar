package dab.poao.nav.no.dokark

import dab.poao.nav.no.azureAuth.logger
import dab.poao.nav.no.pdfgenClient.PdfSuccess
import dab.poao.nav.no.pdfgenClient.PdfgenResult
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
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

class DokarkClient(config: ApplicationConfig) {
    val clientScope = config.property("dokark.client-scope").getString()
    val clientUrl = config.property("dokark.client-url").getString()
    val azureClient = AzureClient(config.toOauthConfig())
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.BODY // TODO: Fjern etter at debugging er ferdig
        }
    }

    suspend fun opprettJournalpost(token: IncomingToken, pdfResult: PdfSuccess, navn: String, fnr: String): DokarkResult {
        val res = client.post("$clientUrl/rest/journalpostapi/v1/journalpost") {
            header("authorization", "Bearer ${azureClient.getOnBehalfOfToken("openid profile $clientScope", token)}")
            contentType(ContentType.Application.Json)
            setBody(dummyJournalpost(pdfResult.pdfByteString, navn, fnr))
        }
        if (!res.status.isSuccess()) {
            logger.warn("Failet å opprette journalpost:", res.bodyAsText())
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