package dab.poao.nav.no.arkivering.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class JournalføringOutbound(
    val sistJournalført: LocalDateTime
)