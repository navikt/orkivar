package dab.poao.nav.no.arkivering.dto

import dab.poao.nav.no.dokark.Fnr
import dab.poao.nav.no.dokark.Navn
import kotlinx.serialization.Serializable

typealias ArkivAktivitetStatus = String

@Serializable
data class ArkiveringsPayload(
    val metadata: ArkiveringsMetadata,
    val aktiviteter: Map<ArkivAktivitetStatus, List<ArkivAktivitet>>,
    val dialogTråder: List<DialogTråd>
)

@Serializable
data class ArkiveringsMetadata (
    val fnr: Fnr,
    val navn: Navn,
)

@Serializable
data class ArkivAktivitet(
    val tittel: String,
    val type: String,
    val status: String,
    val detaljer: List<Detalj>,
    val meldinger: List<Melding>,
//    val tags: List<Tag>
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
data class DialogTråd(
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



