package dab.poao.nav.no.pdfgenClient.dto

import kotlinx.serialization.Serializable

@Serializable
data class PdfgenPayload(
    val navn: String,
    val fnr: String,
    val journalfoeringstidspunkt: String
)
