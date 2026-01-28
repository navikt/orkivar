package dab.poao.nav.no

import dab.poao.nav.no.pdfCaching.NyPdfSomSkalCaches
import dab.poao.nav.no.pdfCaching.PdfCache
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
    fun `Skal lagre pdf uten feil`() {
        val nyPdf = NyPdfSomSkalCaches(
            pdf = "bytearray".toByteArray(),
            fnr = "12345678910",
            veilederIdent = "T123123"
        )
        pdfCache.lagre(nyPdf)
    }

    @Test
    fun `Skal kunne lagre flere PDF-er for samme veileder og bruker`() {
        val pdf1 = NyPdfSomSkalCaches(pdf = "byteArray1".toByteArray(), fnr = "12345678910", veilederIdent = "T123123")
        val pdf2 = pdf1.copy(pdf = "byteArray1".toByteArray())
        val førstePdfUuid = pdfCache.lagre(pdf1)
        val andrePdfUuid = pdfCache.lagre(pdf2)
        pdfCache.hentFraCache(førstePdfUuid) shouldNotBe null
        pdfCache.hentFraCache(andrePdfUuid) shouldNotBe null
    }

    @Test
    fun `Når PDF oppdateres skal UUID oppdateres`() {
        val pdf = NyPdfSomSkalCaches(pdf = "bytearray".toByteArray(), fnr = "12345678910", veilederIdent = "T123123")
        val lagretPdfUuid = pdfCache.lagre(pdf)
        val oppdatertPdfUuid = pdfCache.lagre(pdf.copy(pdf = "annetByteArray".toByteArray()))
        lagretPdfUuid.toString() shouldNotBe oppdatertPdfUuid.toString()
    }

    @Test
    fun `Skal kunne hente pdf uten feil`() {
        val nyPdf = NyPdfSomSkalCaches(pdf = "bytearray".toByteArray(), fnr = "12345678910", veilederIdent = "T123123")
        val lagretPdfUuid = pdfCache.lagre(nyPdf)
        val hentetPdf = pdfCache.hentFraCache(lagretPdfUuid)
        hentetPdf?.pdf shouldBe nyPdf.pdf
    }

    @Test
    fun `Når PDF hentes fra cache skal den også slettes`() {
        val nyPdf = NyPdfSomSkalCaches(pdf = "bytearray".toByteArray(), fnr = "12345678910", veilederIdent = "T123123")
        val lagretPdfUuid = pdfCache.lagre(nyPdf)
        val hentetPdf = pdfCache.hentFraCache(lagretPdfUuid)
        hentetPdf?.pdf shouldBe nyPdf.pdf
        val hentetPdfPåNytt = pdfCache.hentFraCache(lagretPdfUuid)
        hentetPdfPåNytt shouldBe null
    }

    @Test
    fun `Skal kunne slette pdf uten feil`() {
        val pdf = NyPdfSomSkalCaches(pdf = "bytearray".toByteArray(), fnr = "12345678910", veilederIdent = "T123123")
        val lagretPdfUuid = pdfCache.lagre(pdf)
        pdfCache.slett(lagretPdfUuid)
        pdfCache.hentFraCache(lagretPdfUuid) shouldBe null
    }
}
