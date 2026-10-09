package dab.poao.nav.no

import dab.poao.nav.no.pdfgenClient.vaskStringForUgyldigeTegn
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class IsCharacterSupportedTest: StringSpec({

    "Skal ikke vaske bort japanske tegn" {
      "heiら".vaskStringForUgyldigeTegn() shouldBe "heiら"
    }

    "Skal ikke vaske bort ugyldig Downwards Arrow from Bar (U+21A7)" {
      "hei↧".vaskStringForUgyldigeTegn() shouldBe "hei↧"
    }

    "Skal vaske bort vertical tab tegn" {
        "hei\u000B\u000B".vaskStringForUgyldigeTegn() shouldBe "hei"
    }

    "Skal ikke vaske bort emojis" {
        "hei \uD83D\uDE03".vaskStringForUgyldigeTegn() shouldBe "hei \uD83D\uDE03"
    }

    "Skal ikke vaske bort gyldige Unicode-tegn bare fordi de ikke er i Source Sans" {
        "hei \uED15".vaskStringForUgyldigeTegn() shouldBe "hei \uED15"
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

    "Skal ikke vaske bort hjertetegn" {
      "♡".vaskStringForUgyldigeTegn() shouldBe "♡"
    }

    "Skal ikke vaske bort pil tegn" {
      "➢".vaskStringForUgyldigeTegn() shouldBe "➢"
    }

    "Skal ikke vaske bort hand som peker tegn" {
      "☞".vaskStringForUgyldigeTegn() shouldBe "☞"
    }

    "Skal erstatte U+F0B7 med bullet U+2022" {
        "hei \uF0B7".vaskStringForUgyldigeTegn() shouldBe "hei \u2022"
    }

    "Skal vaske bort U+ED5B" {
        "hei\uED5Bder".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal bytte ut ikke kompatibelt høytaler ikon med unicode høytaler" {
        "\uF028".vaskStringForUgyldigeTegn() shouldBe "\uD83D\uDD0A"
    }

    "skal vaske bort rart tegn (usikker på hva det egentlig er)" {
        "\ue72c".vaskStringForUgyldigeTegn() shouldBe ""
    }

    "skal vaske bort rart tegn nr 2 (usikker på hva det egentlig er)" {
        "\uf0fc".vaskStringForUgyldigeTegn() shouldBe ""
    }

    "skal vaske bort rart tegn nr 3 (usikker på hva det egentlig er)" {
        "\ue930".vaskStringForUgyldigeTegn() shouldBe ""
    }

    "skal vaske bort rart tegn nr 4 (usikker på hva det egentlig er)" {
        "\uf0f0".vaskStringForUgyldigeTegn() shouldBe ""
    }
})