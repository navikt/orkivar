package dab.poao.nav.no.database

import org.jetbrains.exposed.dao.CompositeEntity
import org.jetbrains.exposed.dao.CompositeEntityClass
import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.ZonedDateTime
import javax.sql.DataSource
import kotlinx.datetime.LocalDateTime as KotlinxLocalDateTime

class PdfCacheRepository(dataSource: DataSource) {

    init {
        Database.connect(dataSource)
    }

    object PdfCache : CompositeIdTable() {
        val veilederIdent = varchar("veileder_ident", 7)
        val fnr = varchar("fnr", 11)
        val createdAt = datetime("created_at")
        val updatedAt = datetime("created_at")
        val pdf = binary("pdf")
    }

    class CachetPdf(id: EntityID<CompositeID>) : CompositeEntity(id) {
        companion object : CompositeEntityClass<CachetPdf>(PdfCache)
        val veilederIdent by PdfCache.veilederIdent
        val fnr by PdfCache.fnr
        var createdAt by PdfCache.createdAt
        var updatedAt by PdfCache.updatedAt
        var pdf by PdfCache.pdf
    }




    suspend fun lagreJournalfoering(nyPdf: NyPdfSomSkalCaches) {
        transaction {
            PdfCache.upsert {
                it[veilederIdent] = nyPdf.veilederIdent
                it[fnr] = nyPdf.fnr
                it[updatedAt] = KotlinxLocalDateTime.parse(ZonedDateTime.now().toString())
                it[pdf] = nyPdf.pdf
            }
        }
    }
}

data class NyPdfSomSkalCaches(
    val pdf: ByteArray,
    val fnr: String,
    val veilederIdent: String,
)