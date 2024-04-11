package dab.poao.nav.no.arkivering.dto

import dab.poao.nav.no.dokark.Fnr
import dab.poao.nav.no.dokark.Navn
import kotlinx.serialization.Serializable

typealias ArkivAktivitetStatus = String

@Serializable
data class JournalføringsPayload(
    val metadata: Metadata,
    val journalføringsMetadata: JournalføringsMetadata,
    val aktivitetsplanInnhold: AktivitetsplanInnhold
)

@Serializable
data class ForhåndsvisningPayload(
    val metadata: Metadata,
    val aktivitetsplanInnhold: AktivitetsplanInnhold
)

@Serializable
data class AktivitetsplanInnhold(
    val aktiviteter: Map<ArkivAktivitetStatus, List<ArkivAktivitet>>,
    val dialogtråder: List<ArkivDialogtråd>,
    val mål: String?,
)

@Serializable
data class Metadata(
    val fnr: Fnr,
    val navn: Navn,
    val oppfølgingsperiodeStart: String,
    val oppfølgingsperiodeSlutt: String?,
    val oppfølgingsperiodeId: String,
)

@Serializable
data class JournalføringsMetadata(
    val sakId: Long,
    val fagsaksystem: String,
    val journalførendeEnhet: String,
)

@Serializable
data class ArkivAktivitet(
    val tittel: String,
    val type: String,
    val status: String,
    val detaljer: List<Detalj>,
    val meldinger: List<Melding>,
    val etiketter: List<ArkivEtikett>,
    val eksterneHandlinger: List<EksternHandling>,
    val historikk: Historikk,
)

@Serializable
enum class Stil {
    HEL_LINJE,
    HALV_LINJE,
    PARAGRAF,
    LENKE
}

@Serializable
data class Detalj(
    val stil: Stil,
    val tittel: String,
    val tekst: String?
)

@Serializable
data class ArkivDialogtråd(
    val overskrift: String,
    val meldinger: List<Melding>,
    val egenskaper: List<String>
)

@Serializable
data class Melding(
    val avsender: String,
    val sendt: String,
    val lest: Boolean,
    val viktig: Boolean,
    val tekst: String,
)

@Serializable
data class ArkivEtikett(
    val stil: ArkivEtikettStil,
    val tekst: String
)

@Serializable
enum class ArkivEtikettStil {
    AVTALT, // Avtalt - hardkodet farge i frontend
    // Disse kommer fra AKAAS
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
}

@Serializable
data class EksternHandling(
    val tekst: String,
    val subtekst: String?,
    val url: String,
)

@Serializable
data class Historikk(
    val endringer: List<Endring>
)

@Serializable
data class Endring(
    val formattertTidspunkt: String,
    val beskrivelseForVeileder: String,
    val beskrivelseForBruker: String,
)
