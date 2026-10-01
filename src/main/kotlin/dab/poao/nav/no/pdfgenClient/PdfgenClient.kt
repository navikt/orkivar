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
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

sealed interface PdfgenResult
data class FailedPdfGen(val message: String) : PdfgenResult
data class PdfSuccess(val pdfByteString: ByteArray) : PdfgenResult

class PdfgenClient(config: ApplicationConfig, httpClientEngine: HttpClientEngine) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val teamLogsMarker = MarkerFactory.getMarker("TEAM_LOGS")
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
        val jsonPayload = Json.encodeToString(payload).vaskStringForUgyldigeTegn()

        val response = runCatching {
            client.post("$pdfgenUrl/api/v1/genpdf/dab/aktivitetsplan") {
                setBody(jsonPayload)
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
            false -> {
                logger.error(teamLogsMarker, "Feilet å generere pdf, input var: \n$jsonPayload")
                FailedPdfGen("Feilet å generere pdf HTTP: ${response.status.value} - ${response.bodyAsText()}", )
            }
        }
    }
}

fun String.vaskStringForUgyldigeTegn(): String {
   return this
       /* U+F0B7  is a Private Use Area character, not a real standardized Unicode symbol.        ┃
   So it only works if a very specific legacy font defines it (often old                    ┃
   Word/Symbol/Wingdings workflows). */
       .replace("\uF0B7", "\u2022")
       /* U+ED5B  is private-use and has no universal meaning */
       .replace("\uED5B", "")
       /* U+F028  is also Private Use Area (non-standard, font-dependent), so it has the same     ┃
   portability problem.                                                                     ┃
   If this came from an icon font (common), it often means a speaker/volume icon. A good    ┃
   Unicode replacement is  🔊  ( U+1F50A ) or plain text like  [volume] .                   ┃
   For strict PDF/A/UA, replacing/removing  U+F028  is the right approach.  */
       .replace("\uF028", "\uD83D\uDD0A")
       .sanitizeForPdfText("")
}

fun String.sanitizeForPdfText(replacement: String = ""): String {
   val result = StringBuilder()
   var i = 0

   while (i < this.length) {
       val codePoint = this.codePointAt(i)
       val charCount = Character.charCount(codePoint)

       if (isValidPdfTextCodePoint(codePoint)) {
           result.append(this, i, i + charCount)
       } else {
           result.append(replacement)
       }
       i += charCount
   }
   return result.toString()
}

private fun isValidPdfTextCodePoint(codePoint: Int): Boolean {
   return when {
       codePoint == 0x000A ||
           codePoint == 0x000D ||
           codePoint == 0x0009 -> true

       Character.isISOControl(codePoint) -> false

       codePoint in 0xD800..0xDFFF -> false

       codePoint == 0xFFFE ||
           codePoint == 0xFFFF ||
           codePoint in 0xFDD0..0xFDEF -> false

       else -> true
   }
}
