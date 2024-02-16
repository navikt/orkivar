package dab.poao.nav.no

import dab.poao.nav.no.dokark.lagJournalpost
import io.kotest.core.spec.style.StringSpec
import java.time.LocalDateTime

class JournalpostTest: StringSpec({

    "lagJournalpost skal returnere gyldig JSON som joark godtar" {
        val byteArray = "byteArray".toByteArray()
        val navn = "Per Persen"
        val fnr = "17.05.1814"
        val tidspunkt = LocalDateTime.now()

        val json = lagJournalpost(byteArray, navn, fnr, tidspunkt)
    }
})