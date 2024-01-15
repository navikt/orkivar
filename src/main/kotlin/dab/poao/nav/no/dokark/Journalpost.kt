package dab.poao.nav.no.dokark

import kotlinx.serialization.Serializable

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

