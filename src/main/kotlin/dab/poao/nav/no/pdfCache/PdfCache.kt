package dab.poao.nav.no.pdfCache

import kotlinx.datetime.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PdfCache(
    val dataSource: DataSource,
) {
    private val pdfCacheRepository = PdfCacheRepository(dataSource)

    fun lagre(nyPdf: NyPdfSomSkalCaches): PdfFraCache {
        return pdfCacheRepository.lagre(nyPdf)
    }

    fun hentFraCache(uuid: UUID): PdfFraCache {
        return pdfCacheRepository.hent(uuid)
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