package dab.poao.nav.no.dokark

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*


typealias Fnr = String
typealias Navn = String

fun lagJournalpost(journalpostData: JournalpostData): String =
    Json.encodeToString(Journalpost(
        avsenderMottaker = AvsenderMottaker(
            id = journalpostData.fnr,
            navn = journalpostData.navn,
            idType = "FNR"
        ),
        tema = "OPP",
        behandlingstema = "ab0001",
        bruker = Bruker(
            journalpostData.fnr,
            "FNR"
        ),
        datoDokument = journalpostData.tidspunkt.toString(),
        datoMottatt = journalpostData.tidspunkt.toString(),
        dokumenter = listOf(
            Dokument(
                brevkode = "NAV 04-01.04",
                tittel = "Aktivitetsplan og dialog ${journalpostData.oppfølgingsperiodeStart} - ${journalpostData.oppfølgingsperiodeSlutt?.let { it }?: ""}",
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
        journalfoerendeEnhet = "0701",
        journalposttype = "INNGAAENDE",
        kanal = "NAV_NO",
        sak = Sak(
            fagsakId = journalpostData.sakId.toString(),
            fagsaksystem = journalpostData.fagsaksystem,
            sakstype = "FAGSAK"
        ),
        tilleggsopplysninger = listOf(
            Tilleggsopplysninger(
                nokkel = "orkivar",
                verdi = "12345"
            )
        ),
        tittel = "Søknad om dagpenger ved permittering"
    ))

@Serializable
data class Journalpost(
    val avsenderMottaker: AvsenderMottaker? = null,
    val behandlingstema: String? = null,
    val bruker: Bruker? = null,
    val datoDokument: String? = null,
    val datoMottatt: String? = null,
    val dokumenter: List<Dokument>,
    val eksternReferanseId: String? = null,
    val journalfoerendeEnhet: String? = null,
    val journalposttype: String,
    val kanal: String? = null,
    val sak: Sak? = null,
    val tema: String? = null,
    val tilleggsopplysninger: List<Tilleggsopplysninger>? = null,
    val tittel: String? = null,
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
    val brevkode: String,
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

@Serializable
data class Tilleggsopplysninger(
    val nokkel: String,
    val verdi: String,
)

