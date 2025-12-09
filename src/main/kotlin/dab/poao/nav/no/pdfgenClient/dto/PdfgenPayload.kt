package dab.poao.nav.no.pdfgenClient.dto

import dab.poao.nav.no.arkivering.dto.ArkivAktivitetStatus
import dab.poao.nav.no.arkivering.dto.ArkivAktivitet
import dab.poao.nav.no.arkivering.dto.ArkivDialogtråd
import kotlinx.serialization.Serializable

@Serializable
data class PdfgenPayload(
    val navn: String,
    val fnr: String,
    val tekstTilBruker: String?,
    val brukteFiltre: Map<String, List<String>>,
    val oppfølgingsperiodeStart: String,
    val oppfølgingsperiodeSlutt: String?,
    val journalfoeringstidspunkt: String,
    val aktiviteter: Map<ArkivAktivitetStatus, List<ArkivAktivitet>>,
    val dialogtråder: List<ArkivDialogtråd>,
    val mål: String?,
)
