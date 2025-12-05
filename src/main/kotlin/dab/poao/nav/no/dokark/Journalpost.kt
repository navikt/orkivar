package dab.poao.nav.no.dokark

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.format.DateTimeFormatter
import java.util.*


typealias Fnr = String
typealias Navn = String

val norskDatoFormat = DateTimeFormatter.ofPattern("d. MMMM uuuu", Locale.forLanguageTag("no"))

fun lagJournalpost(journalpostData: JournalpostData): Journalpost =
    Journalpost(
        avsenderMottaker = AvsenderMottaker(
            id = journalpostData.fnr,
            navn = journalpostData.navn,
            idType = "FNR"
        ),
        tema = journalpostData.tema,
        bruker = Bruker(
            journalpostData.fnr,
            "FNR"
        ),
        datoDokument = journalpostData.tidspunkt.toString(),
        datoMottatt = journalpostData.tidspunkt.toString(),
        overstyrInnsynsregler = "VISES_MASKINELT_GODKJENT",
        dokumenter = listOf(
            Dokument(
                tittel = "Aktivitetsplan og dialog ${journalpostData.oppfølgingsperiodeStart} - ${journalpostData.oppfølgingsperiodeSlutt?.let { it }?: journalpostData.tidspunkt.format(norskDatoFormat)}",
                dokumentvarianter = listOf(
                    Dokumentvariant(
                        "PDFA",
                        journalpostData.pdf,
                        "ARKIV"
                    )
                )
            )
        ),
        eksternReferanseId = journalpostData.eksternReferanse.toString(),
        journalfoerendeEnhet = journalpostData.journalførendeEnhet,
        journalposttype = "NOTAT",
        sak = Sak(
            fagsakId = journalpostData.sakId.toString(),
            fagsaksystem = journalpostData.fagsaksystem,
            sakstype = "FAGSAK"
        ),
        tittel = "Aktivitetsplan og dialog"
    )

@Serializable
data class Journalpost(
    val avsenderMottaker: AvsenderMottaker,
    val bruker: Bruker,
    val datoDokument: String,
    val datoMottatt: String,
    val overstyrInnsynsregler: String,
    val dokumenter: List<Dokument>,
    val eksternReferanseId: String,
    val journalfoerendeEnhet: String,
    val journalposttype: String,
    val sak: Sak,
    val tema: String,
    val tittel: String,
)

@Serializable
data class AvsenderMottaker(
    val id: String,
    val idType: String,
    val navn: String,
)

@Serializable
data class Bruker(
    val id: String,
    val idType: String,
)

@Serializable
data class Dokument(
    val dokumentvarianter: List<Dokumentvariant>,
    val tittel: String,
)

@Serializable
data class Dokumentvariant(
    val filtype: String,
    val fysiskDokument: ByteArray,
    val variantformat: String,
)

@Serializable
data class Sak(
    val fagsakId: String,
    val fagsaksystem: String,
    val sakstype: String,
)