package dab.poao.nav.no.pdfCaching

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PdfCache(dataSource: DataSource) {
    private val pdfCacheRepository = PdfCacheRepository(dataSource)
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagre(nyPdf: NyPdfSomSkalCaches): PdfFraCache {
        return pdfCacheRepository.lagre(nyPdf)
    }

    fun hentFraCache(uuid: UUID): PdfFraCache? {
        return pdfCacheRepository.hent(uuid)
    }

    fun slett(uuid: UUID) {
        pdfCacheRepository.slett(uuid)
    }

    suspend fun start() = coroutineScope {
        val minutterEnPdfSkalVæreICache = 1L // TODO: Sett opp til 15
        val antallMillisekunderMellomKjøring = 1000L * 60

        launch {
            while (true) {
                try {
                    pdfCacheRepository.slettRaderSomIkkeHarBlittOppdatertEtter(LocalDateTime.now().minusMinutes(minutterEnPdfSkalVæreICache))
                } catch (e: Exception) {
                    logger.error("Feil ved periodisk sletting av cachede PDF-er", e)
                }
                delay(antallMillisekunderMellomKjøring)
            }
        }
    }

}

data class NyPdfSomSkalCaches(
    val pdf: ByteArray,
    val fnr: String,
    val veilederIdent: String,
)

data class PdfFraCache(
    val pdf: ByteArray,
    val uuid: UUID
)