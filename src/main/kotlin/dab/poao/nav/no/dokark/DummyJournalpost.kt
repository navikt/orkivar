package dab.poao.nav.no.dokark

import java.io.File
import java.util.*

typealias Fnr = String
typealias Navn = String

val dummyJournalpost = { fysiskPdf: ByteArray, navn: Navn, fnr: Fnr -> Journalpost(
    avsenderMottaker = AvsenderMottaker(
        id = fnr,
        navn = navn,
        idType = "FNR"
    ),
    tema = "OPP",
    behandlingstema = "ab0001",
    bruker = Bruker(
        fnr,
        "FNR"
    ),
    datoDokument = "2024-01-12T12:43:19.246Z",
    datoMottatt = "2024-01-12T12:43:19.246Z",
    dokumenter = listOf(
        Dokument(
            brevkode = "NAV 04-01.04",
            tittel = "Aktivitetsplan og dialog 20.01.2023-01.01.2024",
            dokumentvarianter = listOf(
                Dokumentvariant(
                    "PDFA",
                     fysiskPdf,
                    "ARKIV")
            )
        )
    ),
    eksternReferanseId = UUID.randomUUID().toString(),
    journalfoerendeEnhet = "0701",
    journalposttype = "INNGAAENDE",
    kanal = "NAV_NO",
    sak = Sak(
        fagsakId = "10695768",
        fagsaksystem = "AO01",
        sakstype = "FAGSAK"
    ),
    tilleggsopplysninger = listOf(Tilleggsopplysninger(
        nokkel = "orkivar",
        verdi = "12345"
    )),
    tittel = "Søknad om dagpenger ved permittering"
)}

