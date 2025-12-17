package dab.poao.nav.no.arkivering.dto

import dab.poao.nav.no.dokark.Fnr
import dab.poao.nav.no.dokark.Navn
import dab.poao.nav.no.pdfgenClient.vaskStringForUgyldigeTegn
import kotlinx.serialization.Serializable

typealias ArkivAktivitetStatus = String

@Serializable
data class PdfData(
    val fnr: Fnr,
    val navn: Navn,
    val tekstTilBruker: String?,
    val journalførendeEnhetNavn: String,
    val brukteFiltre: Map<String, List<String>>,
    val oppfølgingsperiodeStart: String,
    val oppfølgingsperiodeSlutt: String?,
    val aktiviteter: Map<ArkivAktivitetStatus, List<ArkivAktivitet>>,
    val dialogtråder: List<ArkivDialogtråd>,
    val mål: String?,
    val oppfølgingsperiodeId: String,
)

@Serializable
data class JournalføringPayload(
    val journalførendeEnhetId: String,
    val sakId: Long,
    val fagsaksystem: String,
    val tema: String,
    val pdfPayload: PdfData
)

@Serializable
data class SendTilBrukerPayload(
    val journalføringspayload: JournalføringPayload,
    val brukerHarManuellOppfølging: Boolean,
)

@Serializable
data class ArkivAktivitet(
    val tittel: String,
    val type: String,
    val status: String,
    val detaljer: List<Detalj>,
    val dialogtråd: ArkivDialogtråd?,
    val etiketter: List<ArkivEtikett>,
    val eksterneHandlinger: List<EksternHandling>,
    val historikk: Historikk,
    val forhaandsorientering: ArkivFHO?
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
    var tekst: String?
) {
    init {
        tekst = tekst?.vaskStringForUgyldigeTegn()
    }
}

@Serializable
data class ArkivDialogtråd(
    val overskrift: String?,
    val meldinger: List<Melding>,
    val egenskaper: List<String>,
    val indexSisteMeldingLestAvBruker: Int?,
    val tidspunktSistLestAvBruker: String?,
)

@Serializable
data class Melding(
    val avsender: String,
    val sendt: String,
    val lest: Boolean,
    val viktig: Boolean,
    var tekst: String,
) {
    init {
        tekst = tekst.vaskStringForUgyldigeTegn()
    }
}

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
    val beskrivelse: String,
)

@Serializable
data class ArkivFHO(
    val tekst: String,
    val tidspunktLest: String?,
)
