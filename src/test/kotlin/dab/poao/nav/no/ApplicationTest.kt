package dab.poao.nav.no


import dab.poao.nav.no.arkivering.dto.ForhaandsvisningOutbound
import dab.poao.nav.no.database.Repository
import dab.poao.nav.no.dokark.Journalpost
import dab.poao.nav.no.plugins.configureHikariDataSource
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.ktor.utils.io.*
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.json.Json
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.format.DateTimeFormatter
import java.util.*
import javax.sql.DataSource

class ApplicationTest : StringSpec({
    lateinit var testApp: TestApplication
    lateinit var client: HttpClient
    lateinit var mockOAuth2Server: MockOAuth2Server
    lateinit var postgres: EmbeddedPostgres
    lateinit var dataSource: DataSource

    beforeSpec {
        mockOAuth2Server = MockOAuth2Server().also { it.start() }
        postgres = EmbeddedPostgres.start()

        testApp = TestApplication {
            environment {
                val config = doConfig(mockOAuth2Server, postgres)
                dataSource = configureHikariDataSource(config)
            }
            application { module(mockEngine) }
        }
        client = testApp.createClient {
            install(ContentNegotiation) {
                json()
            }
            install(WebSockets)
        }
    }

    beforeEach {
        (mockEngine.requestHistory as MutableList).clear()
    }

    afterSpec {
        testApp.stop()
        mockOAuth2Server.shutdown()
        postgres.close()
    }

    "Forhåndsvisning skal generere og returnere PDF" {
        val repository by lazy { Repository(dataSource) }
        val token = mockOAuth2Server.getAzureToken("G123223")
        val fnr = "01015450300"
        val forslagAktivitet = arkivAktivitet(status = "Forslag", dialogtråd = dialogtråd)
        val avbruttAktivitet = arkivAktivitet(status = "Avbrutt", forhaandsorientering = forhaandsorientering)
        val oppfølgingsperiodeId = UUID.randomUUID().toString()

        val response = client.post("/forhaandsvisning") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "navn": "TRIVIELL SKILPADDE",
                    "fnr": "$fnr",
                    "oppfølgingsperiodeStart": "19 oktober 2021",
                    "oppfølgingsperiodeSlutt": null,
                    "oppfølgingsperiodeId": "$oppfølgingsperiodeId",
                    "aktiviteter": {
                        "Planlagt": [
                            $forslagAktivitet
                        ],
                        "Avbrutt": [
                            $avbruttAktivitet
                        ]
                    },
                    $dialogtråder,
                    $mål
                }
            """.trimIndent()
            )
        }

        response.status shouldBe HttpStatusCode.OK
        response.body<ForhaandsvisningOutbound>()
        repository.hentJournalposter(fnr) shouldHaveSize 0
        mockEngine.requestHistory.filter { pdfgenUrl.contains(it.url.host) } shouldHaveSize 1
    }

    "Journalføring skal generere PDF, sende til Joark og lagre referanse til journalføringen i egen database" {
        val repository by lazy { Repository(dataSource) }
        val token = mockOAuth2Server.getAzureToken("G122123")
        val fnr = "01015450300"
        val forslagAktivitet = arkivAktivitet(status = "Forslag", dialogtråd = dialogtråd)
        val avbruttAktivitet = arkivAktivitet(status = "Avbrutt", forhaandsorientering = forhaandsorientering)
        val sakId = 1000
        val fagsaksystem = "ARBEIDSOPPFOLGING"
        val tema = "OPP"
        val oppfølgingsperiodeId = UUID.randomUUID()
        val journalførendeEnhet = "0303"

        val response = client.post("/arkiver") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "navn": "TRIVIELL SKILPADDE",
                    "fnr": "$fnr",
                    "oppfølgingsperiodeStart": "19 oktober 2021",
                    "oppfølgingsperiodeSlutt": null,
                    "sakId": $sakId, 
                    "fagsaksystem": $fagsaksystem,
                    "tema": "$tema",
                    "oppfølgingsperiodeId": "$oppfølgingsperiodeId",
                    "journalførendeEnhet": "$journalførendeEnhet",
                    "aktiviteter": {
                        "Planlagt": [
                            $forslagAktivitet
                        ],
                        "Avbrutt": [
                            $avbruttAktivitet
                        ]
                    },
                    $dialogtråder,
                    $mål
                }
            """.trimIndent()
            )
        }
        response.status shouldBe HttpStatusCode.OK

        val journalposterIDatabasen = repository.hentJournalposter(fnr)
        journalposterIDatabasen shouldHaveSize 1
        val journalPost = journalposterIDatabasen.first()
        journalPost.journalpostId shouldBe journalpostId
        journalPost.oppfølgingsperiodeId shouldBe oppfølgingsperiodeId

        val opprettet = repository.hentJournalposter(fnr).first().opprettetTidspunkt
        val opprettetFormatert = opprettet.toJavaLocalDateTime().format(norskDatoKlokkeslettFormat)
        val requestsTilPdfgen = mockEngine.requestHistory.filter { pdfgenUrl.contains(it.url.host) }
        requestsTilPdfgen shouldHaveSize 1

        requestsTilPdfgen.first().body.asString() shouldEqualJson """
                {
                    "navn": "TRIVIELL SKILPADDE",
                    "fnr": "$fnr",
                    "oppfølgingsperiodeStart": "19 oktober 2021",
                    "oppfølgingsperiodeSlutt": null,
                    "journalfoeringstidspunkt":"$opprettetFormatert",
                    "aktiviteter": {
                        "Planlagt": [
                            $forslagAktivitet
                        ],
                        "Avbrutt": [
                            $avbruttAktivitet
                        ]
                    },
                    $dialogtråder,
                    $mål
                }
               """.trimMargin()

        val requestsTilJoark = mockEngine.requestHistory.filter { joarkUrl.contains(it.url.host) }
        requestsTilJoark shouldHaveSize 1
        val bodyTilJoark = requestsTilJoark.first().body.asString()
        bodyTilJoark.shouldContainJsonKeyValue("sak.fagsakId", sakId.toString())
        bodyTilJoark.shouldContainJsonKeyValue("eksternReferanseId", journalPost.referanse.toString())
        bodyTilJoark.shouldContainJsonKeyValue("sak.fagsaksystem", fagsaksystem)
        bodyTilJoark.shouldContainJsonKeyValue("journalfoerendeEnhet", journalførendeEnhet)
        bodyTilJoark.shouldContainJsonKeyValue("tittel", "Aktivitetsplan og dialog")
        bodyTilJoark.shouldContainJsonKeyValue("tema", "OPP")
        bodyTilJoark.shouldContainJsonKeyValue("overstyrInnsynsregler", "VISES_MASKINELT_GODKJENT")
    }

    "Feil i request body skal kaste 400" {
        val response = client.post("/arkiver") {
            bearerAuth(mockOAuth2Server.getAzureToken("G122123"))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "enHeltUventetBody": true
                }
            """.trimIndent()
            )
        }
        response.status shouldBe HttpStatusCode.BadRequest
    }
}) {
    companion object {
        private fun ApplicationEnvironmentBuilder.doConfig(
            mockOAuth2Server: MockOAuth2Server,
            postgres: EmbeddedPostgres
        ): MapApplicationConfig {
            val testConfig = MapApplicationConfig(
                "no.nav.security.jwt.issuers.size" to "1",
                "no.nav.security.jwt.issuers.0.issuer_name" to "AzureAD",
                "no.nav.security.jwt.issuers.0.discoveryurl" to "${mockOAuth2Server.wellKnownUrl("AzureAD")}",
                "no.nav.security.jwt.issuers.0.accepted_audience" to "default",
                "azure.client-id" to "default",
                "azure.token-endpoint" to mockOAuth2Server.tokenEndpointUrl("AzureAD").toString(),
                "azure.client-secret" to "clientSecret",
                "dokark.client-url" to joarkUrl,
                "dokark.client-scope" to "dok.scope",
                "orkivar-pdfgen.url" to pdfgenUrl,
                "postgres.host" to "localhost",
                "postgres.port" to postgres.port.toString(),
                "postgres.database-name" to "postgres",
                "postgres.user" to "postgres",
                "postgres.password" to "postgres",
                "postgres.jdbc-url" to "jdbc:postgresql://localhost:${postgres.port}/postgres?user=postgres&password=postgres",
                "cluster" to "local",
            )
            config = testConfig
            return testConfig
        }
    }
}

private val pdfgenUrl = "http://pdf.gen.no"
private val joarkUrl = "http://dok.ark.no"

private val pdfByteArray =
    "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eXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDE5IDAgUiAvUGcgMiAwIFIgL0sgMSAgPj4KZW5kb2JqCjIwIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9USCAvUCAxNyAwIFIgL0sgWyAyNCAwIFIgXSAgPj4KZW5kb2JqCjI0IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDIwIDAgUiAvUGcgMiAwIFIgL0sgMiAgPj4KZW5kb2JqCjIxIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9USCAvUCAxNyAwIFIgL0sgWyAyNSAwIFIgXSAgPj4KZW5kb2JqCjI1IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDIxIDAgUiAvUGcgMiAwIFIgL0sgMyAgPj4KZW5kb2JqCjE2IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9UQm9keSAvUCAxMyAwIFIgL0sgWyAyNiAwIFIgMjcgMCBSIDI4IDAgUiAyOSAwIFIKMzAgMCBSIDMxIDAgUiAzMiAwIFIgMzMgMCBSIDM0IDAgUiAzNSAwIFIgMzYgMCBSIDM3IDAgUiAzOCAwIFIgXSAgPj4KZW5kb2JqCjI2IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9UUiAvUCAxNiAwIFIgL0sgWyAzOSAwIFIgNDAgMCBSIDQxIDAgUiA0MiAwIFIgXSAKPj4KZW5kb2JqCjM5IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9USCAvUCAyNiAwIFIgL0sgWyA0MyAwIFIgXSAgPj4KZW5kb2JqCjQzIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDM5IDAgUiAvUGcgMiAwIFIgL0sgNCAgPj4KZW5kb2JqCjQwIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAyNiAwIFIgL0sgWyA0NCAwIFIgXSAgPj4KZW5kb2JqCjQ0IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDQwIDAgUiAvUGcgMiAwIFIgL0sgNSAgPj4KZW5kb2JqCjQxIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAyNiAwIFIgL0sgWyA0NSAwIFIgXSAgPj4KZW5kb2JqCjQ1IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDQxIDAgUiAvUGcgMiAwIFIgL0sgNiAgPj4KZW5kb2JqCjQyIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAyNiAwIFIgL0sgWyA0NiAwIFIgXSAgPj4KZW5kb2JqCjQ2IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDQyIDAgUiAvUGcgMiAwIFIgL0sgNyAgPj4KZW5kb2JqCjI3IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9UUiAvUCAxNiAwIFIgL0sgWyA0NyAwIFIgNDggMCBSIDQ5IDAgUiA1MCAwIFIgXSAKPj4KZW5kb2JqCjQ3IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9USCAvUCAyNyAwIFIgL0sgWyA1MSAwIFIgXSAgPj4KZW5kb2JqCjUxIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDQ3IDAgUiAvUGcgMiAwIFIgL0sgOCAgPj4KZW5kb2JqCjQ4IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAyNyAwIFIgL0sgWyA1MiAwIFIgXSAgPj4KZW5kb2JqCjUyIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDQ4IDAgUiAvUGcgMiAwIFIgL0sgOSAgPj4KZW5kb2JqCjQ5IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAyNyAwIFIgL0sgWyA1MyAwIFIgXSAgPj4KZW5kb2JqCjUzIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDQ5IDAgUiAvUGcgMiAwIFIgL0sgMTAgID4+CmVuZG9iago1MCAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEQgL1AgMjcgMCBSIC9LIFsgNTQgMCBSIF0gID4+CmVuZG9iago1NCAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCA1MCAwIFIgL1BnIDIgMCBSIC9LIDExICA+PgplbmRvYmoKMjggMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1RSIC9QIDE2IDAgUiAvSyBbIDU1IDAgUiA1NiAwIFIgNTcgMCBSIDU4IDAgUiBdIAo+PgplbmRvYmoKNTUgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1RIIC9QIDI4IDAgUiAvSyBbIDU5IDAgUiBdICA+PgplbmRvYmoKNTkgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1AgL1AgNTUgMCBSIC9QZyAyIDAgUiAvSyAxMiAgPj4KZW5kb2JqCjU2IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAyOCAwIFIgL0sgWyA2MCAwIFIgXSAgPj4KZW5kb2JqCjYwIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDU2IDAgUiAvUGcgMiAwIFIgL0sgMTMgID4+CmVuZG9iago1NyAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEQgL1AgMjggMCBSIC9LIFsgNjEgMCBSIF0gID4+CmVuZG9iago2MSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCA1NyAwIFIgL1BnIDIgMCBSIC9LIDE0ICA+PgplbmRvYmoKNTggMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDI4IDAgUiAvSyBbIDYyIDAgUiBdICA+PgplbmRvYmoKNjIgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1AgL1AgNTggMCBSIC9QZyAyIDAgUiAvSyAxNSAgPj4KZW5kb2JqCjI5IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9UUiAvUCAxNiAwIFIgL0sgWyA2MyAwIFIgNjQgMCBSIDY1IDAgUiA2NiAwIFIgXSAKPj4KZW5kb2JqCjYzIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9USCAvUCAyOSAwIFIgL0sgWyA2NyAwIFIgXSAgPj4KZW5kb2JqCjY3IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDYzIDAgUiAvUGcgMiAwIFIgL0sgMTYgID4+CmVuZG9iago2NCAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEQgL1AgMjkgMCBSIC9LIFsgNjggMCBSIF0gID4+CmVuZG9iago2OCAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCA2NCAwIFIgL1BnIDIgMCBSIC9LIDE3ICA+PgplbmRvYmoKNjUgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDI5IDAgUiAvSyBbIDY5IDAgUiBdICA+PgplbmRvYmoKNjkgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1AgL1AgNjUgMCBSIC9QZyAyIDAgUiAvSyAxOCAgPj4KZW5kb2JqCjY2IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAyOSAwIFIgL0sgWyA3MCAwIFIgXSAgPj4KZW5kb2JqCjcwIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDY2IDAgUiAvUGcgMiAwIFIgL0sgMTkgID4+CmVuZG9iagozMCAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVFIgL1AgMTYgMCBSIC9LIFsgNzEgMCBSIDcyIDAgUiA3MyAwIFIgNzQgMCBSIF0gCj4+CmVuZG9iago3MSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEggL1AgMzAgMCBSIC9LIFsgNzUgMCBSIF0gID4+CmVuZG9iago3NSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCA3MSAwIFIgL1BnIDIgMCBSIC9LIDIwICA+PgplbmRvYmoKNzIgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDMwIDAgUiAvSyBbIDc2IDAgUiBdICA+PgplbmRvYmoKNzYgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1AgL1AgNzIgMCBSIC9QZyAyIDAgUiAvSyAyMSAgPj4KZW5kb2JqCjczIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAzMCAwIFIgL0sgWyA3NyAwIFIgXSAgPj4KZW5kb2JqCjc3IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDczIDAgUiAvUGcgMiAwIFIgL0sgMjIgID4+CmVuZG9iago3NCAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEQgL1AgMzAgMCBSIC9LIFsgNzggMCBSIF0gID4+CmVuZG9iago3OCAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCA3NCAwIFIgL1BnIDIgMCBSIC9LIDIzICA+PgplbmRvYmoKMzEgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1RSIC9QIDE2IDAgUiAvSyBbIDc5IDAgUiA4MCAwIFIgODEgMCBSIDgyIDAgUiBdIAo+PgplbmRvYmoKNzkgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1RIIC9QIDMxIDAgUiAvSyBbIDgzIDAgUiBdICA+PgplbmRvYmoKODMgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1AgL1AgNzkgMCBSIC9QZyAyIDAgUiAvSyAyNCAgPj4KZW5kb2JqCjgwIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAzMSAwIFIgL0sgWyA4NCAwIFIgXSAgPj4KZW5kb2JqCjg0IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDgwIDAgUiAvUGcgMiAwIFIgL0sgMjUgID4+CmVuZG9iago4MSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEQgL1AgMzEgMCBSIC9LIFsgODUgMCBSIF0gID4+CmVuZG9iago4NSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCA4MSAwIFIgL1BnIDIgMCBSIC9LIDI2ICA+PgplbmRvYmoKODIgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDMxIDAgUiAvSyBbIDg2IDAgUiBdICA+PgplbmRvYmoKODYgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1AgL1AgODIgMCBSIC9QZyAyIDAgUiAvSyAyNyAgPj4KZW5kb2JqCjMyIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9UUiAvUCAxNiAwIFIgL0sgWyA4NyAwIFIgODggMCBSIDg5IDAgUiA5MCAwIFIgXSAKPj4KZW5kb2JqCjg3IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9USCAvUCAzMiAwIFIgL0sgWyA5MSAwIFIgXSAgPj4KZW5kb2JqCjkxIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDg3IDAgUiAvUGcgMiAwIFIgL0sgMjggID4+CmVuZG9iago4OCAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEQgL1AgMzIgMCBSIC9LIFsgOTIgMCBSIF0gID4+CmVuZG9iago5MiAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCA4OCAwIFIgL1BnIDIgMCBSIC9LIDI5ICA+PgplbmRvYmoKODkgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDMyIDAgUiAvSyBbIDkzIDAgUiBdICA+PgplbmRvYmoKOTMgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1AgL1AgODkgMCBSIC9QZyAyIDAgUiAvSyAzMCAgPj4KZW5kb2JqCjkwIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAzMiAwIFIgL0sgWyA5NCAwIFIgXSAgPj4KZW5kb2JqCjk0IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDkwIDAgUiAvUGcgMiAwIFIgL0sgMzEgID4+CmVuZG9iagozMyAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVFIgL1AgMTYgMCBSIC9LIFsgOTUgMCBSIDk2IDAgUiA5NyAwIFIgOTggMCBSIF0gCj4+CmVuZG9iago5NSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEggL1AgMzMgMCBSIC9LIFsgOTkgMCBSIF0gID4+CmVuZG9iago5OSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCA5NSAwIFIgL1BnIDIgMCBSIC9LIDMyICA+PgplbmRvYmoKOTYgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDMzIDAgUiAvSyBbIDEwMCAwIFIgXSAgPj4KZW5kb2JqCjEwMCAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCA5NiAwIFIgL1BnIDIgMCBSIC9LIDMzICA+PgplbmRvYmoKOTcgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDMzIDAgUiAvSyBbIDEwMSAwIFIgXSAgPj4KZW5kb2JqCjEwMSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCA5NyAwIFIgL1BnIDIgMCBSIC9LIDM0ICA+PgplbmRvYmoKOTggMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDMzIDAgUiAvSyBbIDEwMiAwIFIgXSAgPj4KZW5kb2JqCjEwMiAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCA5OCAwIFIgL1BnIDIgMCBSIC9LIDM1ICA+PgplbmRvYmoKMzQgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1RSIC9QIDE2IDAgUiAvSyBbIDEwMyAwIFIgMTA0IDAgUiAxMDUgMCBSIDEwNiAwIFIKXSAgPj4KZW5kb2JqCjEwMyAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEggL1AgMzQgMCBSIC9LIFsgMTA3IDAgUiBdICA+PgplbmRvYmoKMTA3IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDEwMyAwIFIgL1BnIDIgMCBSIC9LIDM2ICA+PgplbmRvYmoKMTA0IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAzNCAwIFIgL0sgWyAxMDggMCBSIF0gID4+CmVuZG9iagoxMDggMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1AgL1AgMTA0IDAgUiAvUGcgMiAwIFIgL0sgMzcgID4+CmVuZG9iagoxMDUgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDM0IDAgUiAvSyBbIDEwOSAwIFIgXSAgPj4KZW5kb2JqCjEwOSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCAxMDUgMCBSIC9QZyAyIDAgUiAvSyAzOCAgPj4KZW5kb2JqCjEwNiAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEQgL1AgMzQgMCBSIC9LIFsgMTEwIDAgUiBdICA+PgplbmRvYmoKMTEwIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDEwNiAwIFIgL1BnIDIgMCBSIC9LIDM5ICA+PgplbmRvYmoKMzUgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1RSIC9QIDE2IDAgUiAvSyBbIDExMSAwIFIgMTEyIDAgUiAxMTMgMCBSIDExNCAwIFIKXSAgPj4KZW5kb2JqCjExMSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEggL1AgMzUgMCBSIC9LIFsgMTE1IDAgUiBdICA+PgplbmRvYmoKMTE1IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDExMSAwIFIgL1BnIDIgMCBSIC9LIDQwICA+PgplbmRvYmoKMTEyIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAzNSAwIFIgL0sgWyAxMTYgMCBSIF0gID4+CmVuZG9iagoxMTYgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1AgL1AgMTEyIDAgUiAvUGcgMiAwIFIgL0sgNDEgID4+CmVuZG9iagoxMTMgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDM1IDAgUiAvSyBbIDExNyAwIFIgXSAgPj4KZW5kb2JqCjExNyAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCAxMTMgMCBSIC9QZyAyIDAgUiAvSyA0MiAgPj4KZW5kb2JqCjExNCAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEQgL1AgMzUgMCBSIC9LIFsgMTE4IDAgUiBdICA+PgplbmRvYmoKMTE4IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDExNCAwIFIgL1BnIDIgMCBSIC9LIDQzICA+PgplbmRvYmoKMzYgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1RSIC9QIDE2IDAgUiAvSyBbIDExOSAwIFIgMTIwIDAgUiAxMjEgMCBSIDEyMiAwIFIKXSAgPj4KZW5kb2JqCjExOSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEggL1AgMzYgMCBSIC9LIFsgMTIzIDAgUiBdICA+PgplbmRvYmoKMTIzIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDExOSAwIFIgL1BnIDIgMCBSIC9LIDQ0ICA+PgplbmRvYmoKMTIwIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAzNiAwIFIgL0sgWyAxMjQgMCBSIF0gID4+CmVuZG9iagoxMjQgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1AgL1AgMTIwIDAgUiAvUGcgMiAwIFIgL0sgNDUgID4+CmVuZG9iagoxMjEgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDM2IDAgUiAvSyBbIDEyNSAwIFIgXSAgPj4KZW5kb2JqCjEyNSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCAxMjEgMCBSIC9QZyAyIDAgUiAvSyA0NiAgPj4KZW5kb2JqCjEyMiAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEQgL1AgMzYgMCBSIC9LIFsgMTI2IDAgUiBdICA+PgplbmRvYmoKMTI2IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDEyMiAwIFIgL1BnIDIgMCBSIC9LIDQ3ICA+PgplbmRvYmoKMzcgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1RSIC9QIDE2IDAgUiAvSyBbIDEyNyAwIFIgMTI4IDAgUiAxMjkgMCBSIDEzMCAwIFIKXSAgPj4KZW5kb2JqCjEyNyAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEggL1AgMzcgMCBSIC9LIFsgMTMxIDAgUiBdICA+PgplbmRvYmoKMTMxIDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDEyNyAwIFIgL1BnIDIgMCBSIC9LIDQ4ICA+PgplbmRvYmoKMTI4IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAzNyAwIFIgL0sgWyAxMzIgMCBSIF0gID4+CmVuZG9iagoxMzIgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1AgL1AgMTI4IDAgUiAvUGcgMiAwIFIgL0sgNDkgID4+CmVuZG9iagoxMjkgMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1REIC9QIDM3IDAgUiAvSyBbIDEzMyAwIFIgXSAgPj4KZW5kb2JqCjEzMyAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvUCAvUCAxMjkgMCBSIC9QZyAyIDAgUiAvSyA1MCAgPj4KZW5kb2JqCjEzMCAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEQgL1AgMzcgMCBSIC9LIFsgMTM0IDAgUiBdICA+PgplbmRvYmoKMTM0IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDEzMCAwIFIgL1BnIDIgMCBSIC9LIDUxICA+PgplbmRvYmoKMzggMCBvYmoKPDwgL1R5cGUgL1N0cnVjdEVsZW0gL1MgL1RSIC9QIDE2IDAgUiAvSyBbIDEzNSAwIFIgMTM2IDAgUiAxMzcgMCBSIDEzOCAwIFIKXSAgPj4KZW5kb2JqCjEzNSAwIG9iago8PCAvVHlwZSAvU3RydWN0RWxlbSAvUyAvVEggL1AgMzggMCBSIC9LIFsgMTM5IDAgUiBdICA+PgplbmRvYmoKMTM5IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9QIC9QIDEzNSAwIFIgL1BnIDIgMCBSIC9LIDUyICA+PgplbmRvYmoKMTM2IDAgb2JqCjw8IC9UeXBlIC9TdHJ1Y3RFbGVtIC9TIC9URCAvUCAzOCAwIFIgL0sgWyAxNDAgMCBSIF0gID4+CmVuZG9iagoxNDAgMCBvYmoKPDwgL1R5cGUg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cmQpIC9DcmVhdGlvbkRhdGUgKEQ6MjAyNDAxMTIxMzM3MjBaMDAnMDAnKQovTW9kRGF0ZSAoRDoyMDI0MDExMjEzMzcyMFowMCcwMCcpID4+CmVuZG9iagp4cmVmCjAgMTUxCjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAxODUxNyAwMDAwMCBuIAowMDAwMDA0MjI4IDAwMDAwIG4gCjAwMDAwMTQ4MjYgMDAwMDAgbiAKMDAwMDAwMDAyMiAwMDAwMCBuIAowMDAwMDA0MzQ1IDAwMDAwIG4gCjAwMDAwMDAwMDAgMDAwMDAgbiAKMDAwMDAxOTM1MyAwMDAwMCBuIAowMDAwMDA0NTk0IDAwMDAwIG4gCjAwMDAwMDQ0MTMgMDAwMDAgbiAKMDAwMDAwNDcxMiAwMDAwMCBuIAowMDAwMDA0ODM1IDAwMDAwIG4gCjAwMDAwMDQ3ODEgMDAwMDAgbiAKMDAwMDAwNDkyMCAwMDAwMCBuIAowMDAwMDE0NzU0IDAwMDAwIG4gCjAwMDAwMDUwMDIgMDAwMDAgbiAKMDAwMDAwNTc0NiAwMDAwMCBuIAowMDAwMDA1MDc3IDAwMDAwIG4gCjAwMDAwMDUxNzAgMDAwMDAgbiAKMDAwMDAwNTMxNCAwMDAwMCBuIAowMDAwMDA1NDU4IDAwMDAwIG4gCjAwMDAwMDU2MDIgMDAwMDAgbiAKMDAwMDAwNTI0MiAwMDAwMCBuIAowMDAwMDA1Mzg2IDAwMDAwIG4gCjAwMDAwMDU1MzAgMDAwMDAgbiAKMDAwMDAwNTY3NCAwMDAwMCBuIAowMDAwMDA1OTA1IDAwMDAwIG4gCjAwMDAwMDY1NzQgMDAwMDAgbiAKMDAwMDAwNzI0NSAwMDAwMCBuIAowMDAwMDA3OTE4IDAwMDAwIG4gCjAwMDAwMDg1OTEgMDAwMDAgbiAKMDAwMDAwOTI2NCAwMDAwMCBuIAowMDAwMDA5OTM3IDAwMDAwIG4gCjAwMDAwMTA2MTAgMDAwMDAgbiAKMDAwMDAxMTI4OSAwMDAwMCBuIAowMDAwMDExOTgyIDAwMDAwIG4gCjAwMDAwMTI2NzUgMDAwMDAgbiAKMDAwMDAxMzM2OCAwMDAwMCBuIAowMDAwMDE0MDYxIDAwMDAwIG4gCjAwMDAwMDU5OTggMDAwMDAgbiAKMDAwMDAwNjE0MiAwMDAwMCBuIAowMDAwMDA2Mjg2IDAwMDAwIG4gCjAwMDAwMDY0MzAgMDAwMDAgbiAKMDAwMDAwNjA3MCAwMDAwMCBuIAowMDAwMDA2MjE0IDAwMDAwIG4gCjAwMDAwMDYzNTggMDAwMDAgbiAKMDAwMDAwNjUwMiAwMDAwMCBuIAowMDAwMDA2NjY3IDAwMDAwIG4gCjAwMDAwMDY4MTEgMDAwMDAgbiAKMDAwMDAwNjk1NSAwMDAwMCBuIAowMDAwMDA3MTAwIDAwMDAwIG4gCjAwMDAwMDY3MzkgMDAwMDAgbiAKMDAwMDAwNjg4MyAwMDAwMCBuIAowMDAwMDA3MDI3IDAwMDAwIG4gCjAwMDAwMDcxNzIgMDAwMDAgbiAKMDAwMDAwNzMzOCAwMDAwMCBuIAowMDAwMDA3NDgzIDAwMDAwIG4gCjAwMDAwMDc2MjggMDAwMDAgbiAKMDAwMDAwNzc3MyAwMDAwMCBuIAowMDAwMDA3NDEwIDAwMDAwIG4gCjAwMDAwMDc1NTUgMDAwMDAgbiAKMDAwMDAwNzcwMCAwMDAwMCBuIAowMDAwMDA3ODQ1IDAwMDAwIG4gCjAwMDAwMDgwMTEgMDAwMDAgbiAKMDAwMDAwODE1NiAwMDAwMCBuIAowMDAwMDA4MzAxIDAwMDAwIG4gCjAwMDAwMDg0NDYgMDAwMDAgbiAKMDAwMDAwODA4MyAwMDAwMCBuIAowMDAwMDA4MjI4IDAwMDAwIG4gCjAwMDAwMDgzNzMgMDAwMDAgbiAKMDAwMDAwODUxOCAwMDAwMCBuIAowMDAwMDA4Njg0IDAwMDAwIG4gCjAwMDAwMDg4MjkgMDAwMDAgbiAKMDAwMDAwODk3NCAwMDAwMCBuIAowMDAwMDA5MTE5IDAwMDAwIG4gCjAwMDAwMDg3NTYgMDAwMDAgbiAKMDAwMDAwODkwMSAwMDAwMCBuIAowMDAwMDA5MDQ2IDAwMDAwIG4gCjAwMDAwMDkxOTEgMDAwMDAgbiAKMDAwMDAwOTM1NyAwMDAwMCBuIAowMDAwMDA5NTAyIDAwMDAwIG4gCjAwMDAwMDk2NDcgMDAwMDAgbiAKMDAwMDAwOTc5MiAwMDAwMCBuIAowMDAwMDA5NDI5IDAwMDAwIG4gCjAwMDAwMDk1NzQgMDAwMDAgbiAKMDAwMDAwOTcxOSAwMDAwMCBuIAowMDAwMDA5ODY0IDAwMDAwIG4gCjAwMDAwMTAwMzAgMDAwMDAgbiAKMDAwMDAxMDE3NSAwMDAwMCBuIAowMDAwMDEwMzIwIDAwMDAwIG4gCjAwMDAwMTA0NjUgMDAwMDAgbiAKMDAwMDAxMDEwMiAwMDAwMCBuIAowMDAwMDEwMjQ3IDAwMDAwIG4gCjAwMDAwMTAzOTIgMDAwMDAgbiAKMDAwMDAxMDUzNyAwMDAwMCBuIAowMDAwMDEwNzAzIDAwMDAwIG4gCjAwMDAwMTA4NDggMDAwMDAgbiAKMDAwMDAxMDk5NSAwMDAwMCBuIAowMDAwMDExMTQyIDAwMDAwIG4gCjAwMDAwMTA3NzUgMDAwMDAgbiAKMDAwMDAxMDkyMSAwMDAwMCBuIAowMDAwMDExMDY4IDAwMDAwIG4gCjAwMDAwMTEyMTUgMDAwMDAgbiAKMDAwMDAxMTM4NiAwMDAwMCBuIAowMDAwMDExNTM1IDAwMDAwIG4gCjAwMDAwMTE2ODQgMDAwMDAgbiAKMDAwMDAxMTgzMyAwMDAwMCBuIAowMDAwMDExNDYwIDAwMDAwIG4gCjAwMDAwMTE2MDkgMDAwMDAgbiAKMDAwMDAxMTc1OCAwMDAwMCBuIAowMDAwMDExOTA3IDAwMDAwIG4gCjAwMDAwMTIwNzkgMDAwMDAgbiAKMDAwMDAxMjIyOCAwMDAwMCBuIAowMDAwMDEyMzc3IDAwMDAwIG4gCjAwMDAwMTI1MjYgMDAwMDAgbiAKMDAwMDAxMjE1MyAwMDAwMCBuIAowMDAwMDEyMzAyIDAwMDAwIG4gCjAwMDAwMTI0NTEgMDAwMDAgbiAKMDAwMDAxMjYwMCAwMDAwMCBuIAowMDAwMDEyNzcyIDAwMDAwIG4gCjAwMDAwMTI5MjEgMDAwMDAgbiAKMDAwMDAxMzA3MCAwMDAwMCBuIAowMDAwMDEzMjE5IDAwMDAwIG4gCjAwMDAwMTI4NDYgMDAwMDAgbiAKMDAwMDAxMjk5NSAwMDAwMCBuIAowMDAwMDEzMTQ0IDAwMDAwIG4gCjAwMDAwMTMyOTMgMDAwMDAgbiAKMDAwMDAxMzQ2NSAwMDAwMCBuIAowMDAwMDEzNjE0IDAwMDAwIG4gCjAwMDAwMTM3NjMgMDAwMDAgbiAKMDAwMDAxMzkxMiAwMDAwMCBuIAowMDAwMDEzNTM5IDAwMDAwIG4gCjAwMDAwMTM2ODggMDAwMDAgbiAKMDAwMDAxMzgzNyAwMDAwMCBuIAowMDAwMDEzOTg2IDAwMDAwIG4gCjAwMDAwMTQxNTggMDAwMDAgbiAKMDAwMDAxNDMwNyAwMDAwMCBuIAowMDAwMDE0NDU2IDAwMDAwIG4gCjAwMDAwMTQ2MDUgMDAwMDAgbiAKMDAwMDAxNDIzMiAwMDAwMCBuIAowMDAwMDE0MzgxIDAwMDAwIG4gCjAwMDAwMTQ1MzAgMDAwMDAgbiAKMDAwMDAxNDY3OSAwMDAwMCBuIAowMDAwMDE3OTAxIDAwMDAwIG4gCjAwMDAwMTQ5MTUgMDAwMDAgbiAKMDAwMDAxNTA2NSAwMDAwMCBuIAowMDAwMDE3NzU3IDAwMDAwIG4gCjAwMDAwMjAyMDQgMDAwMDAgbiAKMDAwMDAxOTY2OSAwMDAwMCBuIAowMDAwMDIwNDQyIDAwMDAwIG4gCjAwMDAwMzU4NjQgMDAwMDAgbiAKdHJhaWxlcgo8PCAvU2l6ZSAxNTEgL1Jvb3QgMTQ2IDAgUiAvSW5mbyAxNTAgMCBSIC9JRCBbIDwwODRiN2I3MjIxNTU3YjM2MzA3ZjE3NTJiNjYxMmE5ZT4KPDA4NGI3YjcyMjE1NTdiMzYzMDdmMTc1MmI2NjEyYTllPiBdID4+CnN0YXJ0eHJlZgozNjA4OQolJUVPRgo="

private val mockEngine = MockEngine { request ->
    if (request.url.toString() == "${pdfgenUrl}/api/v1/genpdf/dab/aktivitetsplan") {
        respond(
            content = ByteReadChannel(pdfByteArray),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    } else if (request.url.toString() == "http://dok.ark.no/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true") {
        val journalpost: Journalpost = Json.decodeFromString(request.body.asString())
        val kanFerdigstilles = journalpost.tema != null
                && journalpost.bruker != null
                && journalpost.sak != null
                && journalpost.journalfoerendeEnhet != null
                && journalpost.tittel != null
                && journalpost.avsenderMottaker?.navn != null
                && journalpost.dokumenter.all { it.tittel != null }

        if (!kanFerdigstilles) {
            // Dette vil ikke faktisk føre til BadRequest, men vi ønsker alltid å ferdigstille
            respondBadRequest()
        }
        respond(
            content = ByteReadChannel(dokarkRespons(ferdigstilt = kanFerdigstilles)),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    } else {
        respond(
            content = ByteReadChannel(""),
            status = HttpStatusCode.BadRequest,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
}

private fun MockOAuth2Server.getAzureToken(navIdent: String) =
    issueToken(
        issuerId = "AzureAD",
        subject = navIdent,
        claims = mapOf("NAVident" to navIdent, "oid" to UUID.randomUUID())
    ).serialize()

private fun arkivAktivitet(status: String, dialogtråd: String? = null, forhaandsorientering: String? = null) = """
    {
      "tittel": "tittel",
      "type": "Jobb jeg har nå",
      "status": "$status",
      "detaljer": [
        {
          "stil": "HALV_LINJE",
          "tittel": "Fra dato",
          "tekst": "05 mars 2020"
        },
        {
          "stil": "HALV_LINJE",
          "tittel": "Til dato",
          "tekst": "05 mars 2021"
        },
        {
          "stil": "HALV_LINJE",
          "tittel": "Stillingsandel",
          "tekst": "HELTID"
        },
        {
          "stil": "HALV_LINJE",
          "tittel": "Arbeidsgiver",
          "tekst": "Vikar"
        },
        {
          "stil": "HALV_LINJE",
          "tittel": "Ansettelsesforhold",
          "tekst": "7,5 timer"
        },
        {
          "stil": "PARAGRAF",
          "tittel": "Beskrivelse",
          "tekst": "beskrivelse"
        }
      ],
      "dialogtråd" : ${if (dialogtråd != null) "$dialogtråd" else null},
      "etiketter": [],
      "eksterneHandlinger": [
        {
          "tekst": "Tekst",
          "subtekst": null,
          "url": "http://localhost:8080"
        }
      ],
      "historikk" : {
        "endringer" : [ 
          {
            "formattertTidspunkt" : "25. mars 2024 kl. 08.00",
            "beskrivelse" : "Bruker opprettet aktiviteten"
          } 
        ]
      },
      "forhaandsorientering" :  ${if (forhaandsorientering != null) "$forhaandsorientering" else null}
    }
""".trimIndent()

private val forhaandsorientering = """
    {
        "tekst" : "fho tekst",
        "tidspunktLest" : "5. mars 2024 kl. 14.31"
    }
""".trimIndent()

private val dialogtråd = """
    {
        "overskrift" : "Penger",
        "meldinger" : [{
            "avsender" : "VEILEDER",
            "sendt" : "5. februar 2024 kl. 02.31",
            "lest" : true,
            "viktig" : false,
            "tekst" : "wehfuiehwf\n\nHilsen F_994188 E_994188"
          }, {
            "avsender" : "BRUKER",
            "sendt" : "5. februar 2024 kl. 02.31",
            "lest" : true,
            "viktig" : false,
            "tekst" : "Jada"
         }],
        "egenskaper" : [ ],
       "indexSisteMeldingLestAvBruker" : 0,
       "tidspunktSistLestAvBruker" : "5. mars 2024 kl. 14.31"
    }
""".trimIndent()

private val dialogtråder = """
    "dialogtråder" : [ {
        "overskrift" : "Penger",
        "meldinger" : [ {
          "avsender" : "BRUKER",
          "sendt" : "05 februar 2024 kl. 02.29",
          "lest" : true,
          "viktig" : false,
          "tekst" : "Jeg liker NAV. NAV er snille!"
        } ],
        "egenskaper" : [ ],
       "indexSisteMeldingLestAvBruker" : null,
       "tidspunktSistLestAvBruker" : null
    } ]
""".trimIndent()

private val journalpostId = "12345"

private val mål = """
    "mål" : "Å få meg jobb"
""".trimIndent()

private fun dokarkRespons(ferdigstilt: Boolean) = """
    {
        "journalpostId": "$journalpostId",
        "journalstatus":"ENDELIG",
        "melding": null,
        "journalpostferdigstilt": $ferdigstilt,
        "dokumenter": [{"dokumentInfoId": "12345"}]  
    }
""".trimIndent()


suspend fun OutgoingContent.asString() = this.toByteArray().decodeToString()

private val norskDatoKlokkeslettFormat = DateTimeFormatter.ofPattern("d. MMMM uuuu 'kl.' HH.mm", Locale.forLanguageTag("no"))
