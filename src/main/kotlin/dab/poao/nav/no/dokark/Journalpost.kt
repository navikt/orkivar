package dab.poao.nav.no.dokark

data class Journalpost(
    val avsenderMottaker: AvsenderMottaker,
    val behandlingstema: String,
    val bruker: Bruker,
    val datoDokument: String,
    val datoMottatt: String,
    val dokumenter: List<Dokumenter>,
    val eksternReferanseId: String,
    val journalfoerendeEnhet: String,
    val journalposttype: String,
    val kanal: String,
    val sak: Sak,
    val tema: String,
    val tilleggsopplysninger: List<Tilleggsopplysninger>,
    val tittel: String,
)

data class AvsenderMottaker(
    val id: String,
    val idType: String,
    val navn: String,
)

data class Bruker(
    val id: String,
    val idType: String,
)

data class Dokumenter(
    val brevkode: String,
    val dokumentvarianter: List<Dokumentvarianter>,
    val tittel: String,
)

data class Dokumentvarianter(
    val filtype: String,
    val fysiskDokument: String,
    val variantformat: String,
)

data class Sak(
    val fagsakId: String,
    val fagsaksystem: String,
    val sakstype: String,
)

data class Tilleggsopplysninger(
    val nokkel: String,
    val verdi: String,
)

