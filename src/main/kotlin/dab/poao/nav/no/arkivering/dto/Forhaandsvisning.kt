package dab.poao.nav.no.arkivering.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.util.UUID

@Serializable
data class ForhaandsvisningOutbound(
    val pdf: ByteArray,
    val sistJournalført: LocalDateTime?
)