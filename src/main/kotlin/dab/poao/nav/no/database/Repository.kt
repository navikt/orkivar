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
import java.util.UUID
import javax.sql.DataSource

class Repository(dataSource: DataSource) {

    init {
        Database.connect(dataSource)
    }

    object Journalfoeringer : IntIdTable() {
        val navIdent = varchar("navident", 7)
        val fnr = varchar("foedselsnummer", 11)
        val opprettetTidspunkt = datetime("opprettet_tidspunkt")
        val referanse = uuid("referanse")
        val journalpostId = text("journalpost_id")
        val oppfølgingsperiodeId = uuid("oppfølgingsperiode_id")
        val type = enumerationByName("type", 50, JournalføringType::class)
    }

    class Journalfoering(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Journalfoering>(Journalfoeringer)
        val opprettetTidspunkt by Journalfoeringer.opprettetTidspunkt
        val fnr by Journalfoeringer.fnr
        val navIdent by Journalfoeringer.navIdent
        val referanse by Journalfoeringer.referanse
        val journalpostId by Journalfoeringer.journalpostId
        val oppfølgingsperiodeId by Journalfoeringer.oppfølgingsperiodeId
        val type by Journalfoeringer.type
    }

    suspend fun lagreJournalfoering(nyJournalføring: NyJournalføring) {
        transaction {
            Journalfoeringer.insert {
                it[navIdent] = nyJournalføring.navIdent
                it[fnr] = nyJournalføring.fnr
                it[opprettetTidspunkt] = KotlinxLocalDateTime.parse(nyJournalføring.opprettetTidspunkt.toString())
                it[referanse] = nyJournalføring.referanse
                it[journalpostId] = nyJournalføring.journalpostId
                it[oppfølgingsperiodeId] = nyJournalføring.oppfølgingsperiodeId
                it[type] = nyJournalføring.type
            }
        }
    }

    suspend fun hentJournalposter(fnr: String, type: JournalføringType): List<Journalfoering> {
        return transaction {
            Journalfoering
                .find { (Journalfoeringer.fnr eq fnr) and (Journalfoeringer.type eq type) }
                .toList()
        }
    }

    suspend fun hentJournalposter(oppfølgingsperiodeId: OppfølgingsperiodeId, type: JournalføringType): List<Journalfoering> {
        return transaction {
            Journalfoering
                .find { (Journalfoeringer.oppfølgingsperiodeId eq oppfølgingsperiodeId) and (Journalfoeringer.type eq type)}
                .toList()
        }
    }

    data class NyJournalføring(
        val navIdent: String,
        val fnr: Fnr,
        val opprettetTidspunkt: LocalDateTime,
        val referanse: UUID,
        val journalpostId: String,
        val oppfølgingsperiodeId: OppfølgingsperiodeId,
        val type: JournalføringType,
    )
}

enum class JournalføringType {
    JOURNALFØRING,
    SENDING_TIL_BRUKER
}

typealias OppfølgingsperiodeId = UUID