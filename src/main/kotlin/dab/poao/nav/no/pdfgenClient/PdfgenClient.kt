package dab.poao.nav.no.pdfgenClient

import dab.poao.nav.no.pdfgenClient.dto.PdfgenPayload
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
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
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun generatePdf(payload: PdfgenPayload): PdfgenResult {
        val timesToRetry = 1
        var numberOfRetries = 0

        val result = doGeneratePdf(payload)

        when (result) {
            is FailedPdfGen -> {
              if (numberOfRetries < timesToRetry) {
                  numberOfRetries++
                  return doGeneratePdf(payload)
              } else {
                  return result
              }
            }
            is PdfSuccess -> return PdfSuccess(result.pdfByteString)
        }
    }

    private suspend fun doGeneratePdf(payload: PdfgenPayload): PdfgenResult {
        val response = runCatching {
            client.post("$pdfgenUrl/api/v1/genpdf/dab/aktivitetsplan") {
                setBody(payload)
                contentType(ContentType.Application.Json)
            }
        }
            .onFailure { logger.error("Nettverksfeil?", it) }
            .getOrElse { return FailedPdfGen("Feilet å generere pdf") }
        return when (response.status.isSuccess()) {
            true -> PdfSuccess(response.body())
            false -> FailedPdfGen("Feilet å generere pdf")
        }
    }
}
