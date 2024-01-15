package dab.poao.nav.no.pdfgenClient

import dab.poao.nav.no.pdfgenClient.dto.PdfgenPayload
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*

sealed interface PdfgenResult
data class FailedPdfGen(val message: String) : PdfgenResult
data class PdfSuccess(val pdfByteString: String) : PdfgenResult

class PdfgenClient(config: ApplicationConfig) {
    val pdfgenUrl = config.property("orkivar-pdfgen.url").getString()
    var client = HttpClient()

    suspend fun generatePdf(payload: PdfgenPayload): PdfgenResult {
        val response = client.post("$pdfgenUrl/api/v1/genpdf/dab/aktivitetsplan") {
            setBody(payload)
        }
        return when (response.status.isSuccess()) {
            true -> PdfSuccess(response.bodyAsText())
            false -> FailedPdfGen("Feilet å generere pdf")
        }
    }
}
