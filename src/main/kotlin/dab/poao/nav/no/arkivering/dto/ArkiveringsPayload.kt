package dab.poao.nav.no.arkivering.dto

import dab.poao.nav.no.dokark.Fnr
import dab.poao.nav.no.dokark.Navn
import kotlinx.serialization.Serializable

@Serializable
data class ArkiveringsPayload(
    val metadata: ArkiveringsMetadata,
//    val aktivitesplan: Aktivitetsplan,
//    val dialoger: Dialoger
)

@Serializable
data class ArkiveringsMetadata (
    val fnr: Fnr,
    val navn: Navn,
//    val snapshotTidspunkt: String,
)


