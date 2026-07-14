package dab.poao.nav.no

import dab.poao.nav.no.pdfgenClient.vaskStringForUgyldigeTegn
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class IsCharacterSupportedTest: StringSpec({

     "Skal vaske bort ugyldige japanske tegn" {
        "heiら".vaskStringForUgyldigeTegn() shouldBe "hei"
    }

    "Skal vaske bort ugyldig Downwards Arrow from Bar (U+21A7)" {
        "hei↧".vaskStringForUgyldigeTegn() shouldBe "hei"
    }

    "Skal vaske bort vertical tab tegn" {
        "hei\u000B\u000B".vaskStringForUgyldigeTegn() shouldBe "hei"
    }

    "Skal ikke vaske bort emojis" {
        "hei \uD83D\uDE03".vaskStringForUgyldigeTegn() shouldBe "hei \uD83D\uDE03"
    }

    "Skal ikke vaske rart tegn fra Navet" {
        "hei \uED15".vaskStringForUgyldigeTegn() shouldBe "hei "
    }

    "Skal ikke vaske bort spesialtegn" {
        "åæøöÄ@\\".vaskStringForUgyldigeTegn() shouldBe "åæøöÄ@\\"
    }

    "Skal ikke vaske bort mattematiske uttrykk" {
        "1+2-3=4?#%".vaskStringForUgyldigeTegn() shouldBe "1+2-3=4?#%"
    }

    "skal vaske bort vertical tab" {
        "lol\u000b".vaskStringForUgyldigeTegn() shouldBe "lol"
    }

    "skal ikke vaske bort newlines eller tabs" {
        "\n \t".vaskStringForUgyldigeTegn() shouldBe "\n \t"
    }

    "Skal ikke vaske tegn ofte brukt i markdown" {
        "```#_[]<>0*".vaskStringForUgyldigeTegn() shouldBe "```#_[]<>0*"
    }

    "Skal vaske bort hjertetegn" {
        "♡".vaskStringForUgyldigeTegn() shouldBe ""
    }

    "Skal vaske bort pil tegn" {
        "➢".vaskStringForUgyldigeTegn() shouldBe ""
    }
})