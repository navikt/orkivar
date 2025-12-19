package dab.poao.nav.no.pdfCaching

import java.util.UUID
import javax.sql.DataSource

class PdfCache(dataSource: DataSource) {
    private val pdfCacheRepository = PdfCacheRepository(dataSource)

    fun lagre(nyPdf: NyPdfSomSkalCaches): PdfFraCache {
        return pdfCacheRepository.lagre(nyPdf)
    }

    fun hentFraCache(uuid: UUID): PdfFraCache? {
        return pdfCacheRepository.hent(uuid)
    }

    fun slett(uuid: UUID) {
        pdfCacheRepository.slett(uuid)
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