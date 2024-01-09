package dab.poao.nav.no

import configureAuthentication
import dab.poao.nav.no.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.LoggerFactory
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
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
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
                "azure.client-id" to "clientId",
                "azure.token-endpoint" to "tokenEndpoint",
                "azure.client-secret" to "clientSecret",
                "dokark.client-url" to "http://dok.ark.no",
                "dokark.client-scope" to "dok.scope",
            )
        }
    }
}
