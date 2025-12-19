package dab.poao.nav.no.pdfCache

import javax.sql.DataSource

class PdfCache(
    val dataSource: DataSource,
) {
    private val pdfCacheRepository = PdfCacheRepository(dataSource)

    fun lagre(nyPdf: NyPdfSomSkalCaches) {
        pdfCacheRepository.lagre(nyPdf)
    }
}

data class NyPdfSomSkalCaches(
    val pdf: ByteArray,
    val fnr: String,
    val veilederIdent: String,
)