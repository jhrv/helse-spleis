package no.nav.helse.person

import no.nav.helse.januar
import no.nav.helse.serde.JsonBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ArbeidsgiverOpptjeningsgrunnlagTest {

    @Test
    fun `serialiserer en ting`() {
        val arbeidsgiverOpptjeningsgrunnlag = ArbeidsgiverOpptjeningsgrunnlag(
            orgnummer = "orgnummer",
            arbeidsforhold = listOf(Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = null, deaktivert = false))
        )

        val `"json"` = mutableMapOf<String, Any>()
        val arbeidsgiverOpptjeningsgrunnlagState = JsonBuilder.ArbeidsgiverOpptjeningsgrunnlagState(`"json"`)

        arbeidsgiverOpptjeningsgrunnlag.accept(arbeidsgiverOpptjeningsgrunnlagState)

        assertEquals(
            mapOf(
                "orgnummer" to listOf(
                    mapOf("ansattFom" to 1.januar, "ansattTom" to null, "deaktivert" to false)
                )
            ),
            `"json"`
        )
    }

    @Test
    fun `serialiserer en liste med ting`() {

    }
}
