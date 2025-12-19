package dab.poao.nav.no

import dab.poao.nav.no.pdfCache.NyPdfSomSkalCaches
import dab.poao.nav.no.pdfCache.PdfCache
import io.kotest.matchers.shouldBe
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test

class PdfCacheTest {
    private val dataSource = EmbeddedPostgres.start().postgresDatabase
    private val pdfCache = PdfCache(dataSource)

    init {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }

    @Test
    fun `skal lagre pdf uten feil`() {
        val nyPdf = NyPdfSomSkalCaches(
            pdf = "bytearray".toByteArray(),
            fnr = "12345678910",
            veilederIdent = "T123123"
        )
        pdfCache.lagre(nyPdf)
    }

    @Test
    fun `skal kunne hente pdf uten feil`() {
        val nyPdf = NyPdfSomSkalCaches(pdf = "bytearray".toByteArray(), fnr = "12345678910", veilederIdent = "T123123")
        val lagretPdf = pdfCache.lagre(nyPdf)
        val hentetPdf = pdfCache.hentFraCache(lagretPdf.uuid)
        hentetPdf shouldBe nyPdf
    }
}