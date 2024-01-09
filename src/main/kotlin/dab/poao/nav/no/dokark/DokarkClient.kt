package dab.poao.nav.no.dokark

import dab.poao.nav.no.azureAuth.AzureClient
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*

class DokarkClient(config: ApplicationConfig) {
    val clientScope = config.property("dokark.client-scope").getString()
    val clientUrl = config.property("dokark.client-url").getString()
    val azureClient = AzureClient(config)
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val accessToken = azureClient.getAccessToken(clientScope)
                    BearerTokens(accessToken, "")
                }
                refreshTokens {
                    val accessToken = azureClient.getAccessToken(clientScope)
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