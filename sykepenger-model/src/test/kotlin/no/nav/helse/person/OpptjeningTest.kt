package no.nav.helse.person

import no.nav.helse.desember
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.april
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.oktober
import no.nav.helse.person.etterlevelse.MaskinellJurist
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OpptjeningTest {

    @Test
    fun `Tom liste med arbeidsforhold betyr at du ikke oppfyller opptjeningskrav`() {
        val arbeidsforhold = emptyList<Opptjening.ArbeidsgiverOpptjeningsgrunnlag>()
        val opptjening = Opptjening.opptjening(arbeidsforhold, 1.januar, MaskinellJurist())

        assertFalse(opptjening.erOppfylt())
    }

    @Test
    fun `Én dags opptjening oppfyller ikke krav til opptjening`() {
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = null, deaktivert = false))
            )
        )
        val opptjening = Opptjening.opptjening(arbeidsforhold, 2.januar, MaskinellJurist())

        assertFalse(opptjening.erOppfylt())
    }

    @Test
    fun `27 dager opptjening oppfyller ikke krav til opptjening`() {
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = null, deaktivert = false))
            )
        )
        val opptjening = Opptjening.opptjening(arbeidsforhold, 1.januar.plusDays(27), MaskinellJurist())

        assertFalse(opptjening.erOppfylt())
    }

    @Test
    fun `28 dager opptjening oppfyller krav til opptjening`() {
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = null, deaktivert = false))
            )
        )
        val opptjening = Opptjening.opptjening(arbeidsforhold, 1.januar.plusDays(28), MaskinellJurist())

        assertTrue(opptjening.erOppfylt())
    }

    @Test
    fun `Opptjening skal ikke bruke deaktiverte arbeidsforhold`() {
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = null, deaktivert = true))
            )
        )
        val opptjening = Opptjening.opptjening(arbeidsforhold, 1.januar.plusDays(28), MaskinellJurist())

        assertFalse(opptjening.erOppfylt())
    }

    @Test
    fun `Opptjening skal ikke koble sammen om deaktiverte arbeidsforhold fører til gap`() {
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = 10.januar, deaktivert = false),
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 11.januar, ansattTom = 14.januar, deaktivert = true),
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 15.januar, ansattTom = null, deaktivert = false)
                )
            )
        )

        val opptjening = Opptjening.opptjening(arbeidsforhold, 1.januar.plusDays(28), MaskinellJurist())

        assertFalse(opptjening.erOppfylt())
    }

    @Test
    fun `to tilstøtende arbeidsforhold`() {
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = 10.januar, deaktivert = false),
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 11.januar, ansattTom = null, deaktivert = false)
                )
            )
        )

        val opptjening = Opptjening.opptjening(arbeidsforhold, 1.januar.plusDays(28), MaskinellJurist())

        assertTrue(opptjening.erOppfylt())
    }

    @Test
    fun `Opptjening kobler sammen gap selvom rekkefølgen ikke er kronologisk`() {
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = 10.januar, deaktivert = false),
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 15.januar, ansattTom = null, deaktivert = false),
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 11.januar, ansattTom = 14.januar, deaktivert = false)
                )
            )
        )

        val opptjening = Opptjening.opptjening(arbeidsforhold, 1.januar.plusDays(28), MaskinellJurist())

        assertTrue(opptjening.erOppfylt())
    }

    @Test
    fun `slutter på lørdag, starter på mandag`() {
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "a1",
                listOf(
                    Arbeidsforholdhistorikk.Arbeidsforhold(
                        ansattFom = 1.oktober(2017),
                        ansattTom = 30.april(2022),
                        deaktivert = false
                    )
                )
            ),

            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "a2",
                listOf(
                    Arbeidsforholdhistorikk.Arbeidsforhold(
                        ansattFom = 2.mai(2022),
                        ansattTom = null,
                        deaktivert = false
                    )
                )
            )
        )

        val opptjening = Opptjening.opptjening(arbeidsforhold, 2.mai(2022), MaskinellJurist())

        assertTrue(opptjening.erOppfylt())
        assertEquals(1.oktober(2017), opptjening.opptjeningFom())
    }

    @Test
    fun `slutter på fredag, starter på mandag`() {
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "a1",
                listOf(
                    Arbeidsforholdhistorikk.Arbeidsforhold(
                        ansattFom = 1.oktober(2017),
                        ansattTom = 29.april(2022),
                        deaktivert = false
                    )
                )
            ),

            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "a2",
                listOf(
                    Arbeidsforholdhistorikk.Arbeidsforhold(
                        ansattFom = 2.mai(2022),
                        ansattTom = null,
                        deaktivert = false
                    )
                )
            )
        )

        val opptjening = Opptjening.opptjening(arbeidsforhold, 2.mai(2022), MaskinellJurist())

        assertTrue(opptjening.erOppfylt())
        assertEquals(1.oktober(2017), opptjening.opptjeningFom())
    }

    @Test
    fun `slutter på torsdag, starter på mandag`() {
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "a1",
                listOf(
                    Arbeidsforholdhistorikk.Arbeidsforhold(
                        ansattFom = 1.oktober(2017),
                        ansattTom = 28.april(2022),
                        deaktivert = false
                    )
                )
            ),

            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "a2",
                listOf(
                    Arbeidsforholdhistorikk.Arbeidsforhold(
                        ansattFom = 2.mai(2022),
                        ansattTom = null,
                        deaktivert = false
                    )
                )
            )
        )

        val opptjening = Opptjening.opptjening(arbeidsforhold, 2.mai(2022), MaskinellJurist())

        assertFalse(opptjening.erOppfylt())
        assertEquals(2.mai(2022), opptjening.opptjeningFom())
    }

    @Test
    fun `§ 8-2 ledd 1 - opptjeningstid tilfredstilt`() {
        val jurist = MaskinellJurist()
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                AbstractPersonTest.ORGNUMMER,
                listOf(Arbeidsforholdhistorikk.Arbeidsforhold(4.desember(2017), 31.januar, deaktivert = false))
            )
        )
        Opptjening.opptjening(arbeidsforhold, 1.januar, jurist)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = Paragraf.PARAGRAF_8_2,
            ledd = Ledd.LEDD_1,
            versjon = 12.juni(2020),
            input = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "tilstrekkeligAntallOpptjeningsdager" to 28,
                "arbeidsforhold" to listOf(
                    mapOf(
                        "orgnummer" to AbstractPersonTest.ORGNUMMER,
                        "fom" to 4.desember(2017),
                        "tom" to 31.januar
                    )
                )
            ),
            output = mapOf("antallOpptjeningsdager" to 28)
        )
    }

    @Test
    fun `§ 8-2 ledd 1 - opptjeningstid ikke tilfredstilt`() {
        val jurist = MaskinellJurist()
        val arbeidsforhold = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                AbstractPersonTest.ORGNUMMER,
                listOf(Arbeidsforholdhistorikk.Arbeidsforhold(5.desember(2017), 31.januar, deaktivert = false))
            )
        )
        Opptjening.opptjening(arbeidsforhold, 1.januar, jurist)

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = Paragraf.PARAGRAF_8_2,
            ledd = Ledd.LEDD_1,
            versjon = 12.juni(2020),
            input = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "tilstrekkeligAntallOpptjeningsdager" to 28,
                "arbeidsforhold" to listOf(
                    mapOf(
                        "orgnummer" to AbstractPersonTest.ORGNUMMER,
                        "fom" to 5.desember(2017),
                        "tom" to 31.januar
                    )
                )
            ),
            output = mapOf("antallOpptjeningsdager" to 27)
        )
    }
}
