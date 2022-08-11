package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class DelvisRefusjonRevurderingTest : AbstractEndToEndTest() {

    @Test
    fun `korrigerende inntektsmelding med halvering av inntekt setter riktig refusjonsbeløp fra nyeste inntektsmelding`()  {
        nyttVedtak(1.januar, 31.januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT / 2,
            refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList())
        )
        håndterOverstyrInntekt(INNTEKT / 2, skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 715, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 715, 715, subset = 17.januar til 31.januar)
    }

    @Test
    fun `overstyring av inntekt med økning av inntekt uten nytt refusjonsbeløp`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

        håndterOverstyrInntekt(50000.månedlig, skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING
        )

    }

    @Test
    fun `overstyring av inntekt med nedjustering av inntekt uten nytt refusjonsbeløp`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))
        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

        håndterOverstyrInntekt(INNTEKT/2, skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
        assertUtbetalingsbeløp(1.vedtaksperiode, 715, 1431, subset = 17.januar til 31.januar)
    }

    @Test
    fun `to arbeidsgivere hvor inntekten på den ene senkes slik at den andre arbeidsgiveren får brukerutbetalinger`() = Toggle.RevurdereInntektMedFlereArbeidsgivere.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        val a1Inntekt = 50000.månedlig
        val a2Inntekt = 10000.månedlig

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1, beregnetInntekt = a1Inntekt, refusjon = Inntektsmelding.Refusjon(a1Inntekt, null, emptyList()))
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2, beregnetInntekt = a2Inntekt, refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), a1Inntekt.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), a2Inntekt.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), a1Inntekt.repeat(3)),
                    grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), a2Inntekt.repeat(3))
                )
            , arbeidsforhold = emptyList()
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)

        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 0,
            forventetArbeidsgiverRefusjonsbeløp = 2308,
            subset = 1.januar til 16.januar,
            orgnummer = a1
        )
        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 2161,
            forventetArbeidsgiverRefusjonsbeløp = 2308,
            subset = 17.januar til 31.januar,
            orgnummer = a1
        )
        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 0,
            forventetArbeidsgiverRefusjonsbeløp = 0,
            subset = 1.januar til 16.januar,
            orgnummer = a2
        )
        assertUtbetalingsbeløp(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 0,
            forventetArbeidsgiverRefusjonsbeløp = 0,
            subset = 17.januar til 31.januar,
            orgnummer = a2
        )

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            beregnetInntekt = a1Inntekt / 2,
            refusjon = Inntektsmelding.Refusjon(a1Inntekt / 2, null, emptyList())
        )
        håndterOverstyrInntekt(
            inntekt = a1Inntekt / 2,
            skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, orgnummer = a1, tilstand = AVVENTER_SIMULERING_REVURDERING)
        assertSisteTilstand(1.vedtaksperiode, orgnummer = a2, tilstand = AVVENTER_REVURDERING)
    }
}
