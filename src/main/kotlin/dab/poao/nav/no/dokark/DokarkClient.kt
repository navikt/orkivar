package dab.poao.nav.no.dokark

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import no.nav.poao.dab.ktor_oauth_client.AzureClient
import no.nav.poao.dab.ktor_oauth_client.OauthClientCredentialsConfig

class DokarkClient(config: ApplicationConfig) {
    val clientScope = config.property("dokark.client-scope").getString()
    val clientUrl = config.property("dokark.client-url").getString()
    val azureClient = AzureClient(config.toOauthConfig())
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val accessToken = azureClient.getM2MToken(clientScope)
                    BearerTokens(accessToken, "")
                }
                refreshTokens {
                    val accessToken = azureClient.getM2MToken(clientScope)
                    BearerTokens(accessToken, "")
                }
            }
        }
    }

    suspend fun opprettJournalpost() {
        client.post("$clientUrl/rest/journalpostapi/v1/journalpost") {
            setBody(dummyJournalpost)
        }
    }
}

fun ApplicationConfig.toOauthConfig(): OauthClientCredentialsConfig {
    val azureClientId = this.property("azure.client-id").getString()
    val clientSecret = this.property("azure.client-secret").toString()
    val tokenEndpoint = this.property("azure.token-endpoint").getString()
    return OauthClientCredentialsConfig(
        clientId = azureClientId,
        clientSecret = clientSecret,
        tokenEndpoint = tokenEndpoint
    )
}