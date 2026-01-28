package dab.poao.nav.no.pdfCaching

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotlinx.datetime.LocalDateTime as KotlinxLocalDateTime

class PdfCacheRepository(dataSource: DataSource) {

    init {
        Database.connect(dataSource)
    }

    object CachetPdfTabell : Table("cachet_pdf") {
        val uuid = uuid("uuid")
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
