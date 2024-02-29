package dab.poao.nav.no

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

        val json = lagJournalpost(byteArray, navn, fnr, tidspunkt, eksternReferanseId, sakId)
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
                  "tittel": "Aktivitetsplan og dialog 20.01.2023-01.01.2024"
                }
              ],
              "eksternReferanseId": "$eksternReferanseId",
              "journalfoerendeEnhet": "0701",
              "journalposttype": "INNGAAENDE",
              "kanal": "NAV_NO",
              "sak": {
                "fagsakId": "$sakId",
                "fagsaksystem": "AO01",
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