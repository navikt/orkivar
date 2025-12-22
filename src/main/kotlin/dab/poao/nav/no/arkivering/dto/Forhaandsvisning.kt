package dab.poao.nav.no.arkivering.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ForhaandsvisningOutbound(
    val pdf: ByteArray,
    val sistJournalført: LocalDateTime?,
    val uuidCachetPdf: String?,
)