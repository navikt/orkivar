package dab.poao.nav.no.pdfgenClient

import dab.poao.nav.no.pdfgenClient.dto.PdfgenPayload
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import no.nav.poao.dab.ktor_oauth_client.logger

sealed interface PdfgenResult
data class FailedPdfGen(val message: String) : PdfgenResult
data class PdfSuccess(val pdfByteString: ByteArray) : PdfgenResult

class PdfgenClient(config: ApplicationConfig, httpClientEngine: HttpClientEngine) {
    val pdfgenUrl = config.property("orkivar-pdfgen.url").getString()
    val client = HttpClient(httpClientEngine) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 3000
            socketTimeoutMillis = 30000
        }
        install(ContentNegotiation) { json() }
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 3)
        }
    }

    suspend fun generatePdf(payload: PdfgenPayload): PdfgenResult {
        val response = runCatching {
            client.post("$pdfgenUrl/api/v1/genpdf/dab/aktivitetsplan") {
                setBody(payload)
                contentType(ContentType.Application.Json)
            }
        }
            .onFailure {
                if (it is ConnectTimeoutException) {
                    logger.error("Timeout ved generering av pdf", it)
                } else {
                    logger.error("Uventet feil ved generering av pdf", it)
                }
            }
            .getOrElse { return FailedPdfGen("Feilet å generere pdf: ${it.message}") }
        return when (response.status.isSuccess()) {
            true -> PdfSuccess(response.body())
            false -> FailedPdfGen("Feilet å generere pdf HTTP: ${response.status.value} - ${response.bodyAsText()}", )
        }
    }
}
