package dab.poao.nav.no.pdfgenClient.dto

import dab.poao.nav.no.arkivering.dto.AktivitetStatus
import dab.poao.nav.no.arkivering.dto.ArkivAktivitet
import kotlinx.serialization.Serializable

@Serializable
data class PdfgenPayload(
    val navn: String,
    val fnr: String,
    val journalfoeringstidspunkt: String,
    val aktiviteter: Map<AktivitetStatus, List<ArkivAktivitet>>
)
