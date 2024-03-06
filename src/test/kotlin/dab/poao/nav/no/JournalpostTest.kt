package dab.poao.nav.no

import dab.poao.nav.no.dokark.JournalpostData
import dab.poao.nav.no.dokark.lagJournalpost
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.StringSpec
import java.time.LocalDateTime
import java.util.UUID

class JournalpostTest: StringSpec({

    "lagJournalpost skal returnere gyldig JSON som joark godtar" {
        val byteArray = "byteArray".toByteArray()
        val navn = "Per Persen"
        val fnr = "10022884722"
        val tidspunkt = LocalDateTime.now()
        val eksternReferanseId = UUID.randomUUID()
        val sakId = 2000L
        val fagsaksystem = "ARBEIDSOPPFOLGING"
        val oppfølgingsperiodeStart = "10. juni 2023"
        val oppfølgingsperiodeSlutt = null

        val json = lagJournalpost(JournalpostData(byteArray, navn, fnr, tidspunkt, sakId, fagsaksystem, eksternReferanseId, oppfølgingsperiodeStart, oppfølgingsperiodeSlutt))
        json shouldEqualJson """
            {
              "avsenderMottaker": {
                "id": "$fnr",
                "idType": "FNR",
                "navn": "$navn"
              },
              "behandlingstema": "ab0001",
              "bruker": {
                "id": "$fnr",
                "idType": "FNR"
              },
              "datoDokument": "$tidspunkt",
              "datoMottatt": "$tidspunkt",
              "dokumenter": [
                {
                  "brevkode": "NAV 04-01.04",
                  "dokumentvarianter": [
                    {
                      "filtype": "PDFA",
                      "fysiskDokument": [
                        98,
                        121,
                        116,
                        101,
                        65,
                        114,
                        114,
                        97,
                        121
                      ],
                      "variantformat": "ARKIV"
                    }
                  ],
                  "tittel": "Aktivitetsplan og dialog $oppfølgingsperiodeStart - "
                }
              ],
              "eksternReferanseId": "$eksternReferanseId",
              "journalfoerendeEnhet": "0701",
              "journalposttype": "INNGAAENDE",
              "kanal": "NAV_NO",
              "sak": {
                "fagsakId": "$sakId",
                "fagsaksystem": "$fagsaksystem",
                "sakstype": "FAGSAK"
              },
              "tema": "OPP",
              "tilleggsopplysninger": [
                {
                  "nokkel": "orkivar",
                  "verdi": "12345"
                }
              ],
              "tittel": "Søknad om dagpenger ved permittering"
            }
        """.trimIndent()
    }
})