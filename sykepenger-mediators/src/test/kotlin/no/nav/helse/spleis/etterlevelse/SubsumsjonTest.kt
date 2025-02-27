package no.nav.helse.spleis.etterlevelse

import java.time.LocalDate
import java.time.YearMonth
import no.nav.helse.desember
import no.nav.helse.januar
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.TestMessageFactory.Subsumsjon
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SubsumsjonTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Sender § 8-15 ved deaktivering av ghostarbeidsforhold`() {
        val a2 = "ag2"

        tilGodkjenningMedGhost(a2 = a2)
        sendOverstyringArbeidsforhold(1.januar, listOf(TestMessageFactory.ArbeidsforholdOverstyrt(
            a2,
            true,
            "Jeg, en saksbehandler, overstyrte pga 8-15"
        )))

        val subsumsjon = testRapid.inspektør.meldinger("subsumsjon")
            .map { it["subsumsjon"] }
            .first { it["paragraf"].asText() == "8-15" }

        assertEquals("ag2", subsumsjon["input"]["organisasjonsnummer"].asText())
        assertEquals(1.januar, subsumsjon["input"]["skjæringstidspunkt"].asLocalDate())
        val actualInntekterSisteTreMåneder = subsumsjon["input"]["inntekterSisteTreMåneder"].toList()

        actualInntekterSisteTreMåneder.forEachIndexed { index, inntekt ->
            assertEquals(YearMonth.of(2017, 10).plusMonths(index.toLong()), inntekt["årMåned"].asYearMonth())
            assertEquals(1000.0, inntekt["beløp"].asDouble())
            assertEquals("LØNNSINNTEKT", inntekt["type"].asText())
            assertEquals("kontantytelse", inntekt["fordel"].asText())
            assertEquals("fastloenn", inntekt["beskrivelse"].asText())
        }

        assertEquals("Jeg, en saksbehandler, overstyrte pga 8-15", subsumsjon["input"]["forklaring"].asText())

        assertEquals(a2, subsumsjon["output"]["arbeidsforholdAvbrutt"].asText())
        assertEquals("VILKAR_OPPFYLT", subsumsjon["utfall"].asText())
    }

    @Test
    fun `Sender § 8-15 ved reaktivering av ghostarbeidsforhold`() {
        val a2 = "ag2"

        tilGodkjenningMedGhost(a2 = a2)
        sendOverstyringArbeidsforhold(1.januar, listOf(TestMessageFactory.ArbeidsforholdOverstyrt(
            a2,
            false,
            "Jeg, en saksbehandler, overstyrte pga 8-15"
        )))

        val subsumsjon = testRapid.inspektør.meldinger("subsumsjon")
            .map { it["subsumsjon"] }
            .first { it["paragraf"].asText() == "8-15" }

        assertEquals("ag2", subsumsjon["input"]["organisasjonsnummer"].asText())
        assertEquals(1.januar, subsumsjon["input"]["skjæringstidspunkt"].asLocalDate())
        val actualInntekterSisteTreMåneder = subsumsjon["input"]["inntekterSisteTreMåneder"].toList()

        actualInntekterSisteTreMåneder.forEachIndexed { index, inntekt ->
            assertEquals(YearMonth.of(2017, 10).plusMonths(index.toLong()), inntekt["årMåned"].asYearMonth())
            assertEquals(1000.0, inntekt["beløp"].asDouble())
            assertEquals("LØNNSINNTEKT", inntekt["type"].asText())
            assertEquals("kontantytelse", inntekt["fordel"].asText())
            assertEquals("fastloenn", inntekt["beskrivelse"].asText())
        }

        assertEquals("Jeg, en saksbehandler, overstyrte pga 8-15", subsumsjon["input"]["forklaring"].asText())

        assertEquals(a2, subsumsjon["output"]["aktivtArbeidsforhold"].asText())
        assertEquals("VILKAR_IKKE_OPPFYLT", subsumsjon["utfall"].asText())
    }

    @Test
    fun `subsumsjon-hendelser - med toggle`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)

        val subsumsjoner = testRapid.inspektør.meldinger("subsumsjon").map { it["subsumsjon"] }
        assertTrue(subsumsjoner.isNotEmpty())
        val subsumsjon = subsumsjoner.first { it["paragraf"].asText() == "8-17" }
        assertEquals("a", subsumsjon["bokstav"].asText())
        assertTrue(subsumsjon["ledd"].isInt)
        assertEquals(1, subsumsjon["ledd"].asInt())
    }

    @Test
    fun `Sender § 8-28 (3) b ved overstyring av ghost-inntekt`() {
        val a2 = "ag2"

        tilGodkjenningMedGhost(a2 = a2)
        sendOverstyringInntekt(1100.0, 1.januar, Subsumsjon("8-28", "3", "b"), a2, "Jeg, en saksbehandler, overstyrte pga 8-28 b")
        val subsumsjon = testRapid.inspektør.meldinger("subsumsjon")
            .map { it["subsumsjon"] }
            .first { it["paragraf"].asText() == "8-28" && it["bokstav"].asText() == "b" }

        assertEquals(3, subsumsjon["ledd"].asInt())
        assertEquals("ag2", subsumsjon["input"]["organisasjonsnummer"].asText())
        assertEquals(1.januar, subsumsjon["input"]["skjæringstidspunkt"].asLocalDate())
        assertEquals(1.desember(2017), subsumsjon["input"]["startdatoArbeidsforhold"].asLocalDate())
        assertEquals(1.januar, subsumsjon["input"]["overstyrtInntektFraSaksbehandler"]["dato"].asLocalDate())
        assertEquals(1100.0, subsumsjon["input"]["overstyrtInntektFraSaksbehandler"]["beløp"].asDouble())
        assertEquals("Jeg, en saksbehandler, overstyrte pga 8-28 b", subsumsjon["input"]["forklaring"].asText())

        assertEquals(13200.0, subsumsjon["output"]["beregnetGrunnlagForSykepengegrunnlagPrÅr"].asDouble())
        assertEquals(1100.0, subsumsjon["output"]["beregnetGrunnlagForSykepengegrunnlagPrMåned"].asDouble())
    }
    @Test
    fun `Sender § 8-28 (3) c ved overstyring av ghost-inntekt`() {

        val a2 = "ag2"

        tilGodkjenningMedGhost(a2 = a2)
        sendOverstyringInntekt(1100.0, 1.januar, Subsumsjon("8-28", "3", "c"), a2, "Jeg, en saksbehandler, overstyrte pga 8-28 c")
        val subsumsjon = testRapid.inspektør.meldinger("subsumsjon")
            .map { it["subsumsjon"] }
            .first { it["paragraf"].asText() == "8-28" && it["bokstav"].asText() == "c" }

        assertEquals(3, subsumsjon["ledd"].asInt())
        assertEquals("ag2", subsumsjon["input"]["organisasjonsnummer"].asText())
        assertEquals(1.januar, subsumsjon["input"]["skjæringstidspunkt"].asLocalDate())
        assertEquals(1.januar, subsumsjon["input"]["overstyrtInntektFraSaksbehandler"]["dato"].asLocalDate())
        assertEquals(1100.0, subsumsjon["input"]["overstyrtInntektFraSaksbehandler"]["beløp"].asDouble())
        assertEquals("Jeg, en saksbehandler, overstyrte pga 8-28 c", subsumsjon["input"]["forklaring"].asText())

        assertEquals(13200.0, subsumsjon["output"]["beregnetGrunnlagForSykepengegrunnlagPrÅr"].asDouble())
        assertEquals(1100.0, subsumsjon["output"]["beregnetGrunnlagForSykepengegrunnlagPrMåned"].asDouble())

    }

    @Test
    fun `Sender § 8-28 (5) ved overstyring av ghost-inntekt`() {

        val a2 = "ag2"

        tilGodkjenningMedGhost(a2 = a2)
        sendOverstyringInntekt(1100.0, 1.januar, Subsumsjon("8-28", "5", null), a2, "Jeg, en saksbehandler, overstyrte pga 8-28 (5)")
        val subsumsjon = testRapid.inspektør.meldinger("subsumsjon")
            .map { it["subsumsjon"] }
            .first { it["paragraf"].asText() == "8-28" && it["ledd"].asText() == "5" }

        assertNull(subsumsjon["bokstav"])
        assertEquals("ag2", subsumsjon["input"]["organisasjonsnummer"].asText())
        assertEquals(1.januar, subsumsjon["input"]["skjæringstidspunkt"].asLocalDate())
        assertEquals(1.januar, subsumsjon["input"]["overstyrtInntektFraSaksbehandler"]["dato"].asLocalDate())
        assertEquals(1100.0, subsumsjon["input"]["overstyrtInntektFraSaksbehandler"]["beløp"].asDouble())
        assertEquals("Jeg, en saksbehandler, overstyrte pga 8-28 (5)", subsumsjon["input"]["forklaring"].asText())

        assertEquals(13200.0, subsumsjon["output"]["beregnetGrunnlagForSykepengegrunnlagPrÅr"].asDouble())
        assertEquals(1100.0, subsumsjon["output"]["beregnetGrunnlagForSykepengegrunnlagPrMåned"].asDouble())
    }


    private fun tilGodkjenningMedGhost(a1: String = "ag1", a2: String = "ag2", fom: LocalDate = 1.januar, tom: LocalDate = 31.januar) {
        sendNySøknad(SoknadsperiodeDTO(fom = fom, tom = tom, sykmeldingsgrad = 100), orgnummer = a1)
        sendSøknad(listOf(SoknadsperiodeDTO(fom = fom, tom = tom, sykmeldingsgrad = 100)), orgnummer = a1)
        sendInntektsmelding(listOf(Periode(fom, fom.plusDays(15))), fom, orgnummer = a1)
        sendYtelser(0, orgnummer = a1)
        sendVilkårsgrunnlag(
            vedtaksperiodeIndeks = 0,
            skjæringstidspunkt = fom,
            orgnummer = a1,
            inntekter = sammenligningsgrunnlag(
                fom, listOf(
                    TestMessageFactory.InntekterForSammenligningsgrunnlagFraLøsning.Inntekt(INNTEKT, a1),
                    TestMessageFactory.InntekterForSammenligningsgrunnlagFraLøsning.Inntekt(1000.0, a2),
                )
            ),
            arbeidsforhold = listOf(
                TestMessageFactory.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                TestMessageFactory.Arbeidsforhold(a2, fom.minusMonths(1), null)
            ),
            inntekterForSykepengegrunnlag = sykepengegrunnlag(
                fom, listOf(
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(INNTEKT, a1),
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(1000.0, a2),
                )
            )
        )
        sendYtelser(0, orgnummer = a1)
        sendSimulering(0, orgnummer = a1, status = SimuleringMessage.Simuleringstatus.OK)
    }
}
