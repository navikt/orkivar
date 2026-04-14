package dab.poao.nav.no

import dab.poao.nav.no.pdfCaching.NyPdfSomSkalCaches
import dab.poao.nav.no.pdfCaching.PdfCacheRepository
import dab.poao.nav.no.plugins.configureFlyway
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PdfRepositoryTest {
    private val dataSource = EmbeddedPostgres.start().postgresDatabase
    private val pdfCacheRepository = PdfCacheRepository(dataSource)

    init {
        configureFlyway(dataSource)
    }

    @Test
    fun `Skal kunne slette en PDF-cache ikke oppdatert etter gitt tidspunkt`() {
        val pdf1 = pdfCacheRepository.lagre(NyPdfSomSkalCaches(pdf = "bytearray1".toByteArray(), fnr = "12345678910", veilederIdent = "T123123"))
        Thread.sleep(300)
        val pdf2 = pdfCacheRepository.lagre(NyPdfSomSkalCaches(pdf = "bytearray1".toByteArray(), fnr = "12345678910", veilederIdent = "T123123"))
        val _200MillisekunderINanos = 200000000L
        pdfCacheRepository.slettRaderSomIkkeHarBlittOppdatertEtter(LocalDateTime.now().minusNanos(_200MillisekunderINanos))
        pdfCacheRepository.hent(pdf1.uuid) shouldBe null
        pdfCacheRepository.hent(pdf2.uuid) shouldNotBe  null
    }
}
