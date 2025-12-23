package dab.poao.nav.no.pdfCaching

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PdfCache(dataSource: DataSource) {
    private val pdfCacheRepository = PdfCacheRepository(dataSource)
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    init {
        startPeriodiskSletting()
    }

    fun lagre(nyPdf: NyPdfSomSkalCaches): UUID {
        return pdfCacheRepository.lagre(nyPdf).uuid
    }

    fun hentFraCache(uuid: UUID): PdfFraCache? {
        return pdfCacheRepository.hent(uuid)
    }

    fun slett(uuid: UUID) {
        pdfCacheRepository.slett(uuid)
    }

    private fun startPeriodiskSletting() {
        val minutterEnPdfSkalVæreICache = 10L
        val antallMillisekunderMellomKjøring = 1000L * 60

        scope.launch {
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