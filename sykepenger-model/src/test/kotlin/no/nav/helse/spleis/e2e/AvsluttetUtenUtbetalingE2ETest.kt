package no.nav.helse.spleis.e2e

import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AvsluttetUtenUtbetalingE2ETest: AbstractEndToEndTest() {
    /*
        Hvis vi har en kort periode som har endt opp i AVSLUTTET_UTEN_UTBETALING vil alle etterkommende perioder
        bli stuck med å vente på den korte perioden. Da vil de aldri komme seg videre og til slutt time ut
    */
    @Test
    fun `kort periode blokkerer neste periode i ny arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 10.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
        )

        håndterSykmelding(Sykmeldingsperiode(3.mars, 26.mars, 100.prosent))
        val inntektsmeldingId = håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(3.mars, 18.mars)))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(3.mars, 26.mars, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    /*
        Denne testen er en slags følgefeil av testen over. Det at periode #2 er kort og får inntektsmeldingen lurer oss ut av UFERDIG-løpet og lar oss
        fortsette behandling. Dessverre setter vi oss fast i AVVENTER_HISTORIKK fordi periode #1 blokkerer utførelsen i Vedtaksperiode.forsøkUtbetaling(..)
     */
    @Test
    fun `kort periode setter senere periode fast i AVVENTER_HISTORIKK`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 10.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
        )

        håndterSykmelding(Sykmeldingsperiode(3.mars, 7.mars, 100.prosent))
        håndterSøknad(Sykdom(3.mars, 7.mars, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
        )

        håndterSykmelding(Sykmeldingsperiode(8.mars, 26.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(3.vedtaksperiode, arbeidsgiverperioder = listOf(Periode(3.mars, 18.mars)))

        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)

        håndterSøknadMedValidering(3.vedtaksperiode, Sykdom(8.mars, 26.mars, 100.prosent))
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode, INNTEKT)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Sender vedtaksperiode_endret når inntektsmelidng kommer i AVSLUTTET_UTEN_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2021), 1.januar(2021), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2021), 1.januar(2021), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        assertEquals(1, observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)).size)

        val hendelseId = håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021))

        assertEquals(2, observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)).size)
        assertTrue(hendelseId in observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)))

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `ny inntektsmelding med første fraværsdag i en sammenhengende periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        val imId = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 23.januar)
        håndterSykmelding(Sykmeldingsperiode(11.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(imId, 2.vedtaksperiode.id(ORGNUMMER))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        assertWarning("Vi har mottatt en inntektsmelding i en løpende sykmeldingsperiode med oppgitt første/bestemmende fraværsdag som er ulik tidligere fastsatt skjæringstidspunkt.", 2.vedtaksperiode.filter())
        assertWarning("Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode.", 2.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        val tidslinje = inspektør.sykdomstidslinje
        assertTrue((1.januar til 31.januar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag })
    }

    @Test
    fun `arbeidsgiverperiode med brudd i helg`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(4.januar, 5.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(8.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(8.januar, 12.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(13.januar, 19.januar, 100.prosent))
        håndterSøknad(Sykdom(13.januar, 19.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(20.januar, 1.februar, 100.prosent))
        håndterSøknad(Sykdom(20.januar, 1.februar, 100.prosent))
        håndterUtbetalingshistorikk(4.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(4.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        // 6. og 7. januar blir FriskHelg og medfører brudd i arbeidsgiverperioden
        // og dermed ble også skjæringstidspunktet forskjøvet til 8. januar
        håndterInntektsmelding(
            listOf(
                1.januar til 3.januar, //3
                4.januar til 5.januar, // 2
                // 6. og 7. januar er helg
                8.januar til 12.januar,// 5
                13.januar til 18.januar // 6
            ), 8.januar
        )
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(4.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
    }
}
