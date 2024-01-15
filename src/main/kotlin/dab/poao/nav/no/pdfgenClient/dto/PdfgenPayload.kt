package dab.poao.nav.no.pdfgenClient.dto

data class PdfgenPayload(
    val navn: String,
    val fnr: String,
    val journalfoeringstidspunkt: String
)
