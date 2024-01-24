package dab.poao.nav.no.arkivering.dto

import dab.poao.nav.no.dokark.Fnr
import dab.poao.nav.no.dokark.Navn
import kotlinx.serialization.Serializable

@Serializable
data class ArkiveringsPayload(
    val metadata: ArkiveringsMetadata,
    val aktiviteter: List<ArkivAktivitet>
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



