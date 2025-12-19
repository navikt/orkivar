package dab.poao.nav.no.pdfCaching

import org.jetbrains.exposed.dao.CompositeEntity
import org.jetbrains.exposed.dao.CompositeEntityClass
import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsertReturning
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotlinx.datetime.LocalDateTime as KotlinxLocalDateTime

class PdfCacheRepository(dataSource: DataSource) {

    init {
        Database.connect(dataSource)
    }

    object CachetPdfTabell : CompositeIdTable("cachet_pdf") {
        val veilederIdent = varchar("veileder_ident", 7)
        val fnr = varchar("fnr", 11)
        val createdAt = datetime("created_at")
        val updatedAt = datetime("updated_at")
        val pdf = binary("pdf")
        val uuid = uuid("uuid")

        init {
            uniqueIndex(veilederIdent, fnr)
        }
    }

    class CachetPdf(id: EntityID<CompositeID>) : CompositeEntity(id) {
        companion object : CompositeEntityClass<CachetPdf>(CachetPdfTabell)

        val veilederIdent by CachetPdfTabell.veilederIdent
        val fnr by CachetPdfTabell.fnr
        val createdAt by CachetPdfTabell.createdAt
        val updatedAt by CachetPdfTabell.updatedAt
        val pdf by CachetPdfTabell.pdf
        val uuid by CachetPdfTabell.uuid
    }

    fun lagre(nyPdf: NyPdfSomSkalCaches): PdfFraCache {
        return transaction {
            CachetPdfTabell.upsertReturning(keys = arrayOf(CachetPdfTabell.veilederIdent, CachetPdfTabell.fnr)) {
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