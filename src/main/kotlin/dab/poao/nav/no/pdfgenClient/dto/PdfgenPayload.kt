package dab.poao.nav.no.pdfgenClient.dto

import dab.poao.nav.no.arkivering.dto.ArkivAktivitetStatus
import dab.poao.nav.no.arkivering.dto.ArkivAktivitet
import dab.poao.nav.no.arkivering.dto.DialogTråd
import kotlinx.serialization.Serializable

@Serializable
data class PdfgenPayload(
    val navn: String,
    val fnr: String,
    val journalfoeringstidspunkt: String,
    val aktiviteter: Map<ArkivAktivitetStatus, List<ArkivAktivitet>>,
    val dialogTråder: List<DialogTråd>
)
