package dab.poao.nav.no.database

import dab.poao.nav.no.dokark.Fnr
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZonedDateTime
import javax.sql.DataSource

class Repository(dataSource: DataSource) {

    init {
        Database.connect(dataSource)
    }

    private object Journalfoering : Table() {
        val id = integer("id").autoIncrement()
        val navIdent = varchar("navident", 7)
        val fnr = varchar("foedselsnummer", 11)
        val opprettetTidspunkt = datetime("opprettet_tidspunkt")

        override val primaryKey = PrimaryKey(id)
    }

    fun lagreJournalfoering(navIdent: String, fnr: Fnr) {
        transaction {
            Journalfoering.insert {
                it[Journalfoering.navIdent] = navIdent
                it[Journalfoering.fnr] = fnr
            }
        }
    }

}