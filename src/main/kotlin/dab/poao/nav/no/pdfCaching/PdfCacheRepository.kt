package dab.poao.nav.no.pdfCaching

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotlinx.datetime.LocalDateTime as KotlinxLocalDateTime

class PdfCacheRepository(dataSource: DataSource) {

    init {
        Database.connect(dataSource)
    }

    object CachetPdfTabell : Table("cachet_pdf") {
        val uuid = javaUUID("uuid")
        val veilederIdent = varchar("veileder_ident", 7)
        val fnr = varchar("fnr", 11)
        val createdAt = datetime("created_at")
        val updatedAt = datetime("updated_at")
        val pdf = binary("pdf")
    }

    fun lagre(nyPdf: NyPdfSomSkalCaches): PdfFraCache {
        return transaction {
            CachetPdfTabell.insertReturning(returning = listOf(CachetPdfTabell.uuid, CachetPdfTabell.pdf)) {
                it[veilederIdent] = nyPdf.veilederIdent
                it[fnr] = nyPdf.fnr
                it[updatedAt] = KotlinxLocalDateTime.parse(LocalDateTime.now().toString())
                it[pdf] = nyPdf.pdf
                it[uuid] = UUID.randomUUID()
            }.single().mapTilCachetPdf()
        }
    }

    fun hent(uuid: UUID): PdfFraCache? {
        return transaction {
            CachetPdfTabell.selectAll().where { CachetPdfTabell.uuid eq uuid }.singleOrNull()?.mapTilCachetPdf()
        }
    }

    fun slett(uuid: UUID) {
        transaction {
            CachetPdfTabell.deleteWhere { CachetPdfTabell.uuid eq uuid }
        }
    }

    fun slettRaderSomIkkeHarBlittOppdatertEtter(tidspunkt: LocalDateTime) {
        transaction {
            CachetPdfTabell.deleteWhere { CachetPdfTabell.updatedAt.less(KotlinxLocalDateTime.parse(tidspunkt.toString())) }
        }
    }

    fun ResultRow.mapTilCachetPdf(): PdfFraCache {
        return PdfFraCache(
            uuid = this[CachetPdfTabell.uuid],
            pdf = this[CachetPdfTabell.pdf]
        )
    }
}
