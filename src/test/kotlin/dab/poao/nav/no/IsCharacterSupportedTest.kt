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

    "Skal erstatte U+2981 med bullet U+2022" {
      "hei\u2981".vaskStringForUgyldigeTegn() shouldBe "hei\u2022"
    }

    "Skal erstatte U+F06F med tom firkant U+25A1" {
        "hei \uF06F".vaskStringForUgyldigeTegn() shouldBe "hei \u25A1"
    }

    "Skal erstatte U+F0A7 med fylt firkant U+25A0" {
        "hei \uF0A7".vaskStringForUgyldigeTegn() shouldBe "hei \u25A0"
    }

    "Skal erstatte U+F076 med bullet U+2022" {
        "hei \uF076".vaskStringForUgyldigeTegn() shouldBe "hei \u2022"
    }

    "Skal erstatte U+F06C med bullet U+2022" {
        "hei \uF06C".vaskStringForUgyldigeTegn() shouldBe "hei \u2022"
    }

    "Skal erstatte fete sans-serif Unicode-bokstaver med vanlige bokstaver" {
        "𝗔𝗕𝗖𝗗𝗘𝗙𝗚𝗛𝗜𝗝𝗞𝗟𝗠𝗡𝗢𝗣𝗤𝗥𝗦𝗧𝗨𝗩𝗪𝗫𝗬𝗭".vaskStringForUgyldigeTegn() shouldBe
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        "𝗮𝗯𝗰𝗱𝗲𝗳𝗴𝗵𝗶𝗷𝗸𝗹𝗺𝗻𝗼𝗽𝗾𝗿𝘀𝘁𝘂𝘃𝘄𝘅𝘆𝘇".vaskStringForUgyldigeTegn() shouldBe
            "abcdefghijklmnopqrstuvwxyz"
    }

    "Skal normalisere andre matematiske bokstavstiler og tall" {
        "𝐀𝑏𝒄𝔇𝕖𝙁𝚐 𝟬𝟭𝟮𝟯𝟰𝟱𝟲𝟳𝟴𝟵".vaskStringForUgyldigeTegn() shouldBe "AbcDeFg 0123456789"
    }

    "Skal ikke normalisere tegn utenfor matematiske alfanumeriske symboler" {
        "² ½ \u212B".vaskStringForUgyldigeTegn() shouldBe "² ½ \u212B"
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

    "Skal vaske bort U+E70E" {
        "hei\uE70Eder".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+E350" {
        "hei\uE350der".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+F04A" {
        "hei\uF04Ader".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+E316" {
        "hei\uE316der".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+E3B8" {
        "hei\uE3B8der".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+E0AC" {
        "hei\uE0ACder".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+F02A" {
        "hei\uF02Ader".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+E4CC" {
        "hei\uE4CCder".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+EAE4" {
        "hei\uEAE4der".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+E370" {
        "hei\uE370der".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+E4F0" {
        "hei\uE4F0der".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+EB24" {
        "hei\uEB24der".vaskStringForUgyldigeTegn() shouldBe "heider"
    }

    "Skal vaske bort U+ED3F" {
        "hei\uED3Fder".vaskStringForUgyldigeTegn() shouldBe "heider"
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