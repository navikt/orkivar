package dab.poao.nav.no.pdfCache

import org.jetbrains.exposed.dao.CompositeEntity
import org.jetbrains.exposed.dao.CompositeEntityClass
import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDateTime
import javax.sql.DataSource
import kotlinx.datetime.LocalDateTime as KotlinxLocalDateTime

class PdfCacheRepository(dataSource: DataSource) {

    init {
        Database.connect(dataSource)
    }

    object PdfCache : CompositeIdTable("cachet_pdf") {
        val veilederIdent = varchar("veileder_ident", 7)
        val fnr = varchar("fnr", 11)
        val createdAt = datetime("created_at")
        val updatedAt = datetime("updated_at")
        val pdf = binary("pdf")

        init {
            uniqueIndex(veilederIdent, fnr)
        }
    }

    class CachetPdf(id: EntityID<CompositeID>) : CompositeEntity(id) {
        companion object : CompositeEntityClass<CachetPdf>(PdfCache)

        val veilederIdent by PdfCache.veilederIdent
        val fnr by PdfCache.fnr
        var createdAt by PdfCache.createdAt
        var updatedAt by PdfCache.updatedAt
        var pdf by PdfCache.pdf
    }


    fun lagre(nyPdf: NyPdfSomSkalCaches) {
        transaction {
            PdfCache.upsert(keys = arrayOf(PdfCache.veilederIdent, PdfCache.fnr)) {
                it[veilederIdent] = nyPdf.veilederIdent
                it[fnr] = nyPdf.fnr
                it[updatedAt] = KotlinxLocalDateTime.parse(LocalDateTime.now().toString())
                it[pdf] = nyPdf.pdf
            }
        }
    }
}