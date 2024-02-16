package dab.poao.nav.no.dokark

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*


typealias Fnr = String
typealias Navn = String

fun lagJournalpost(fysiskPdf: ByteArray, navn: Navn, fnr: Fnr, datoDokument: LocalDateTime) =
    Journalpost(
        avsenderMottaker = AvsenderMottaker(
            id = fnr,
            navn = navn,
            idType = "FNR"
        ),
        tema = "OPP",
        behandlingstema = "ab0001",
        bruker = Bruker(
            fnr,
            "FNR"
        ),
        datoDokument = datoDokument.toString(),
        datoMottatt = datoDokument.toString(),
        dokumenter = listOf(
            Dokument(
                brevkode = "NAV 04-01.04",
                tittel = "Aktivitetsplan og dialog 20.01.2023-01.01.2024",
                dokumentvarianter = listOf(
                    Dokumentvariant(
                        "PDFA",
                        fysiskPdf,
                        "ARKIV"
                    )
                )
            )
        ),
        eksternReferanseId = UUID.randomUUID().toString(),
        journalfoerendeEnhet = "0701",
        journalposttype = "INNGAAENDE",
        kanal = "NAV_NO",
        sak = Sak(
            fagsakId = "10695768",
            fagsaksystem = "AO01",
            sakstype = "FAGSAK"
        ),
        tilleggsopplysninger = listOf(
            Tilleggsopplysninger(
                nokkel = "orkivar",
                verdi = "12345"
            )
        ),
        tittel = "Søknad om dagpenger ved permittering"
    )

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

