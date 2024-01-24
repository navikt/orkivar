package dab.poao.nav.no.database

import dab.poao.nav.no.dokark.Fnr
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import kotlinx.datetime.LocalDateTime as KotlinxLocalDateTime
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class Repository(dataSource: DataSource) {

    init {
        Database.connect(dataSource)
    }

    object Journalfoeringer : IntIdTable() {
        val navIdent = varchar("navident", 7)
        val fnr = varchar("foedselsnummer", 11)
        val opprettetTidspunkt = datetime("opprettet_tidspunkt")
    }

    class Journalfoering(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Journalfoering>(Journalfoeringer)
        val opprettetTidspunkt by Journalfoeringer.opprettetTidspunkt
        val fnr by Journalfoeringer.fnr
        val navIdent by Journalfoeringer.navIdent
    }

    suspend fun lagreJournalfoering(navIdent: String, fnr: Fnr, opprettetTidspunkt: LocalDateTime) {
        transaction {
            Journalfoeringer.insert {
                it[Journalfoeringer.navIdent] = navIdent
                it[Journalfoeringer.fnr] = fnr
                it[Journalfoeringer.opprettetTidspunkt] = KotlinxLocalDateTime.parse(opprettetTidspunkt.toString())
            }
        }
    }

    suspend fun hentJournalposter(fnr: String): List<Journalfoering> {
        return transaction {
            Journalfoering
                .find { Journalfoeringer.fnr eq fnr }
                .toList()
        }
    }

}