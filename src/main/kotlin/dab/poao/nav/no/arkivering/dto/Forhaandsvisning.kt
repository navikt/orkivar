package dab.poao.nav.no.arkivering.dto

import java.util.UUID

data class ForhaandsvisningOutbound(
    val uuid: UUID,
    val pdf: ByteArray
)