package dab.poao.nav.no.dokark

import java.io.File

class pdfaleser {
    fun hentpdf(): ByteArray {
        val pdfurl = this::class.java.classLoader.getResource("eksempel-pdfa.pdf")
        return File(pdfurl.toURI()).readBytes()
    }
}

val dummyJournalpost = Journalpost(
    avsenderMottaker = AvsenderMottaker(
        id = "01117400200",
        navn = "Raus Trane",
        idType = "FNR"
    ),
    tema = "OPP",
    behandlingstema = "ab0001",
    bruker = Bruker(
        "01117400200",
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
                    pdfaleser().hentpdf().toString(),
                    "ARKIV")
            )
        )
    ),
    eksternReferanseId = "a0f480a3-8ab2-4c56-8c93-e53bb35bec2b",
    journalfoerendeEnhet = "0701",
    journalposttype = "INNGAAENDE",
    kanal = "NAV_NO",
    sak = Sak(
        fagsakId = "10695768",
        fagsaksystem = "AO01",
        sakstype = "FAGSAK"
    ),
    tilleggsopplysninger = listOf(Tilleggsopplysninger(
        nokkel = "bucid",
        verdi = "12345"
    )),
    tittel = "Søknad om dagpenger ved permittering"
)