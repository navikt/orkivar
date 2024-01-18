package dab.poao.nav.no

import configureAuthentication
import dab.poao.nav.no.arkivering.dto.ArkiveringsMetadata
import dab.poao.nav.no.arkivering.dto.ArkiveringsPayload
import dab.poao.nav.no.plugins.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.http.json
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.test.*

private val logger = LoggerFactory.getLogger("dab.poao.nav.no.ApplicationTest.kt")
class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        environment { doConfig() }
        application {
            configureAuthentication()
            configureRouting()
        }
        val client = HttpClient().apply {
            install(ContentNegotiation) {
                json()
            }
        }

        client.post("/arkiver") {
            bearerAuth(getAzureToken("G123123"))
            header("Content-Type", "application/json; charset=UTF-8")
            setBody(ArkiveringsPayload(ArkiveringsMetadata(fnr = "123456789", navn = "Navn Navnesen")))

//            setBody("""
//                {
//                  "metadata": {
//                    "navn": "TRIVIELL SKILPADDE",
//                    "fnr": "01015450300"
//                  }
//                }
//            """.trimIndent())
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    companion object {
        val server: MockOAuth2Server by lazy {
            MockOAuth2Server()
                .also { it.start() }
        }
        private fun ApplicationEngineEnvironmentBuilder.doConfig(
            acceptedIssuer: String = "default",
            acceptedAudience: String = "default"
        ) {
            config = MapApplicationConfig(
                "no.nav.security.jwt.issuers.size" to "1",
                "no.nav.security.jwt.issuers.0.issuer_name" to acceptedIssuer,
                "no.nav.security.jwt.issuers.0.discoveryurl" to "${server.wellKnownUrl(acceptedIssuer)}",
                "no.nav.security.jwt.issuers.0.accepted_audience" to acceptedAudience,
                "azure.client-id" to acceptedAudience,
                "azure.token-endpoint" to server.tokenEndpointUrl(acceptedIssuer).toString(),
                "azure.client-secret" to "clientSecret",
                "dokark.client-url" to "http://dok.ark.no",
                "dokark.client-scope" to "dok.scope",
                "orkivar-pdfgen.url" to "http://pdf.gen.no"
            )
        }
    }
}

fun getAzureToken(navIdent: String) = ApplicationTest.server
    .issueToken(
        subject = navIdent,
        claims = mapOf("NAVident" to navIdent, "oid" to UUID.randomUUID()))
    .serialize()
