package dab.poao.nav.no

import dab.poao.nav.no.pdfgenClient.vaskStringForUgyldigeTegn
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class IsCharacterSupportedTest {

    @Test
    fun `Skal vaske bort ugyldige japanske tegn`() {
        "heiら".vaskStringForUgyldigeTegn() shouldBe "hei"
    }

    @Test
    fun `Skal vaske bort ugyldig Downwards Arrow from Bar (U+21A7)`() {
        "hei↧".vaskStringForUgyldigeTegn() shouldBe "hei"
    }

    @Test
    fun `Skal vaske bort vertical tab tegn`() {
        "hei\u000B\u000B".vaskStringForUgyldigeTegn() shouldBe "hei"
    }

    @Test
    fun `Skal ikke vaske bort emojis`() {
        "hei \uD83D\uDE03".vaskStringForUgyldigeTegn() shouldBe "hei \uD83D\uDE03"
    }

    @Test
    fun `Skal ikke vaske rart tegn fra Navet`() {
        "hei \uED15".vaskStringForUgyldigeTegn() shouldBe "hei "
    }

    @Test
    fun `Skal ikke vaske bort spesialtegn`() {
        "åæøöÄ@\\".vaskStringForUgyldigeTegn() shouldBe "åæøöÄ@\\"
    }

    @Test
    fun `Skal ikke vaske bort mattematiske uttrykk`() {
        "1+2-3=4?#%".vaskStringForUgyldigeTegn() shouldBe "1+2-3=4?#%"
    }

    @Test
    fun `skal vaske bort vertical tab`() {
        "lol\u000b".vaskStringForUgyldigeTegn() shouldBe "lol"
    }

    @Test
    fun `skal ikke vaske bort newlines eller tabs`() {
        "\n \t".vaskStringForUgyldigeTegn() shouldBe "\n \t"
    }

    @Test
    fun `Skal ikke vaske tegn ofte brukt i markdown`() {
        "```#_[]<>0*".vaskStringForUgyldigeTegn() shouldBe "```#_[]<>0*"
    }
}