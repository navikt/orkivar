package dab.poao.nav.no.arkivering.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class SistJournalFørtOutboundDto(
    val oppfølgingsperiodeId: String,
    val sistJournalført: LocalDateTime
)
