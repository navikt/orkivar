package dab.poao.nav.no.arkivering.dto

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ForhaandsvisningOutbound(
    val pdf: ByteArray
)