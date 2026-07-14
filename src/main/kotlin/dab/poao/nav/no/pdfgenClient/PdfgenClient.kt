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
import no.nav.poao.dab.ktor_oauth_client.logger
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
    return this.sanitizeForTypstSourceSans("")
}

/*
* "Source Sans Pro" is currently the font used in the PDFs, this function checks if
* a given character is supported by that font,
* */
fun Char.isSupportedBySourceSansPro(): Boolean {
    return true;
}

fun String.sanitizeForTypstSourceSans(replacement: String = ""): String {
    val result = StringBuilder()
    var i = 0

    while (i < this.length) {
        val codePoint = this.codePointAt(i)
        val charCount = Character.charCount(codePoint)

        if (isValidForSourceSansOrEmoji(codePoint)) {
            // Keep the character, emoji, newline, or tab
            result.append(this, i, i + charCount)
        } else {
            // Drop it or replace it with a safe fallback placeholder
            result.append(replacement)
        }
        i += charCount
    }
    return result.toString()
}

private fun isValidForSourceSansOrEmoji(codePoint: Int): Boolean {
    return when {
        // --- 0. WHITESPACE & STRUCTURAL CONTROL ---
        codePoint == 0x000A || // Newline (\n)
                codePoint == 0x000D || // Carriage Return (\r)
                codePoint == 0x0009 -> true // Tab (\t)

        // --- 1. SOURCE SANS PRO NATIVE MAP ---
        codePoint in 0x0020..0x007E || codePoint in 0x00A0..0x00FF -> true // Latin Base & Supplement
        codePoint in 0x0100..0x024F || codePoint in 0x1E00..0x1EFF -> true // Extended Latin
        codePoint in 0x0250..0x02AF || codePoint in 0x02B0..0x02FF -> true // IPA & Modifiers
        codePoint in 0x0300..0x036F -> true                               // Combining Marks
        codePoint in 0x0370..0x03FF -> true                               // Greek & Coptic
        codePoint in 0x0400..0x052F -> true                               // Cyrillic & Supplement
        codePoint in 0x2000..0x206F || codePoint in 0x20A0..0x20CF -> true // General Punctuation & Currencies

        // Specific UI Symbology from Source Sans
        codePoint in listOf(0x2112, 0x2113, 0x2116, 0x2120, 0x2122, 0x2126, 0x212E, 0x214F) -> true
        codePoint in 0x2190..0x2199 || codePoint in listOf(0x21D2, 0x21D4) -> true
        codePoint in listOf(
            0x2202, 0x2206, 0x220F, 0x2211, 0x2212, 0x2215, 0x221A, 0x221E,
            0x222B, 0x2248, 0x2260, 0x2261, 0x2264, 0x2265, 0x22CA
        ) -> true
        codePoint in listOf(0x25A0, 0x25A1, 0x25B2, 0x25B6, 0x25BC, 0x25C0, 0x25CA, 0x25CB) -> true

        // --- 2. TYPST EMOJI FALLBACK MAP ---
        codePoint in 0x1F600..0x1F64F -> true // Emoticons (😃, 😭, etc.)
        codePoint in 0x1F300..0x1F5FF -> true // Miscellaneous Symbols & Pictographs (🎒, 🎬)
        codePoint in 0x1F680..0x1F6FF -> true // Transport & Map Symbols (🚀, 🚗)
        codePoint in 0x1F900..0x1F9FF -> true // Supplemental Symbols & Pictographs (🦊, 🌮)
        codePoint in 0x1FA70..0x1FAFF -> true // Symbols and Pictographs Extended-A (🪓, 🪵)
        codePoint in 0x2600..0x26FF && codePoint != 0x2661 -> true // Misc Symbols (⚡, ⚽, ⚠️), excluding ♡
        codePoint in 0x2700..0x27BF && codePoint != 0x27A2 -> true // // Dingbats (✨, ❌), exclude ➢

        // Reject everything else (like ↧ / U+21A7 or unsupported non-Latin scripts)
        else -> false
    }
}
