package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.etterspurteBehov
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.inspectors.søppelbøtte
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Periodetype
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.september
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class ForlengelseFraInfotrygdTest : AbstractEndToEndTest() {

    @Test
    fun `setter riktig skjæringstidspunkt`() {
        val historikk1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 29.januar(2018), 18.februar(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 19.februar(2018), 18.mars(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 19.mars(2018), 2.april(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.april(2018), 14.mai(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 15.mai(2018), 3.juni(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 4.juni(2018), 22.juni(2018), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 18.mars(2020), 31.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.april(2020), 30.april(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.mai(2020), 31.mai(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning1 = listOf(
            Inntektsopplysning(ORGNUMMER, 18.mars(2020), INNTEKT, true),
            Inntektsopplysning(ORGNUMMER, 29.januar(2018), INNTEKT, true)
        )

        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historikk1.toTypedArray(), inntektshistorikk = inntektsopplysning1)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val historikk2 = historikk1 + listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.juli(2020), 31.august(2020), 100.prosent, 1000.daglig),
        )
        val inntektsopplysning2 = inntektsopplysning1 + listOf(
            Inntektsopplysning(ORGNUMMER, 1.juli(2020), INNTEKT, true)
        )

        assertEquals(18.mars(2020), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertForventetFeil(
            forklaring = "Perioden burde blitt markert som en overgang fra infotrygd",
            nå = {
                assertEquals(Periodetype.INFOTRYGDFORLENGELSE, inspektør.periodetype(1.vedtaksperiode))
            },
            ønsket = {
                assertEquals(Periodetype.OVERGANG_FRA_IT, inspektør.periodetype(1.vedtaksperiode))
            }
        )

        håndterSykmelding(Sykmeldingsperiode(1.september(2020), 30.september(2020), 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.september(2020), 30.september(2020), 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historikk2.toTypedArray(), inntektshistorikk = inntektsopplysning2)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertEquals(18.mars(2020), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertForventetFeil(
            forklaring = "Perioden burde blitt markert som en overgang fra infotrygd",
            nå = {
                assertEquals(Periodetype.INFOTRYGDFORLENGELSE, inspektør.periodetype(2.vedtaksperiode))
            },
            ønsket = {
                assertEquals(Periodetype.OVERGANG_FRA_IT, inspektør.periodetype(2.vedtaksperiode))
            }
        )
    }

    @Test
    fun `oppdager forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 19.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.februar, 25.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(26.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 19.februar, 100.prosent))
        håndterSøknad(Sykdom(20.februar, 25.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)
        ))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `forlenger vedtaksperiode som har gått til infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 3.januar, INNTEKT, true))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser(2.vedtaksperiode)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertEquals(3.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(Periodetype.OVERGANG_FRA_IT, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `forlenger ikke vedtaksperiode som har gått til infotrygd, der utbetaling ikke er gjort`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 19.januar, 25.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 19.januar(2018), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)  // <-- TIL_INFOTRYGD
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 23.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 29.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )

        assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `avdekker tilstøtende periode i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 23.februar, 100.prosent))
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntekter = listOf(
            Inntektsopplysning(
                ORGNUMMER,
                3.januar,
                INNTEKT,
                true
            )
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        assertTilstander(
            0, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING
        )
        assertEquals(3.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
    }

    @Test
    fun `når en periode går Til Infotrygd avsluttes påfølgende, tilstøtende perioder også`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(18.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterUtbetalingshistorikk(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, 1000.daglig)
        )  // <-- TIL_INFOTRYGD
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TIL_INFOTRYGD
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `tidligere utbetalinger i spleis som er forkastet blir tatt med som en del av utbetalingshistorikken`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
            assertEquals(1, it.size)
            assertEquals(Periode(1.januar, 31.januar), it.first())
        }

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)
        inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
            assertEquals(1, it.size)
            assertEquals(Periode(1.januar, 31.januar), it.first())
        }

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            3.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true))
        )
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
            assertEquals(2, it.size)
            assertEquals(Periode(1.januar, 31.januar), it[0])
            assertEquals(Periode(1.mars, 31.mars), it[1])
        }

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
            AVSLUTTET
        )
        assertTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertEquals(28.desember, inspektør.sisteMaksdato(1.vedtaksperiode))
        assertEquals(inspektør.sisteMaksdato(3.vedtaksperiode), inspektør.sisteMaksdato(1.vedtaksperiode))
    }

    @Test
    fun `lager ikke ny arbeidsgiverperiode når det er tilstøtende historikk`() {
        håndterSykmelding(Sykmeldingsperiode(18.februar(2020), 3.mars(2020), 100.prosent))
        håndterSøknad(Sykdom(18.februar(2020), 3.mars(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(4.mars(2020), 17.mars(2020), 100.prosent))
        håndterSøknad(Sykdom(4.mars(2020), 17.mars(2020), 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(18.mars(2020), 15.april(2020), 70.prosent))
        håndterSøknad(Sykdom(18.mars(2020), 15.april(2020), 70.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterInntektsmelding(listOf(Periode(18.februar(2020), 4.mars(2020))), 18.februar(2020))

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.februar(2019) til 1.januar(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, AVVENTER_GODKJENNING, LocalDateTime.now().minusDays(110)) // <-- TIL_INFOTRYGD

        håndterSykmelding(Sykmeldingsperiode(16.april(2020), 7.mai(2020), 100.prosent))
        håndterSøknad(Sykdom(16.april(2020), 7.mai(2020), 100.prosent))

        håndterUtbetalingshistorikk(
            4.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.mars(2020), 17.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 18.mars(2020), 15.april(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    ORGNUMMER,
                    5.mars(2020), INNTEKT, true
                )
            )
        )
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            TIL_INFOTRYGD
        )
        assertTilstander(
            4.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        inspektør.utbetalinger.aktive().also { utbetalinger ->
            assertEquals(1, utbetalinger.size)
            utbetalinger.first().utbetalingstidslinje().inspektør.also {
                assertEquals(15, it.arbeidsgiverperiodeDagTeller)
                assertEquals(16, it.navDagTeller)
            }
        }
    }

    @Test
    fun `Forlengelse av søknad uten utbetaling med opphold betalt i Infotrygd`() {
        // Inspirert av et case i P der en overlappende sykmelding ble kastet
        håndterSykmelding(Sykmeldingsperiode(26.mai(2020), 2.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(26.mai(2020), 2.juni(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(26.mai(2020), 2.juni(2020))), førsteFraværsdag = 26.mai(2020))

        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(22.juni(2020), 11.juli(2020), 100.prosent))

        val historikk = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 11.juni(2020), 21.juni(2020), 100.prosent, 1000.daglig))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 11.juni(2020), INNTEKT, true))

        håndterUtbetalingshistorikk(2.vedtaksperiode, *historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser(2.vedtaksperiode)

        assertTrue(person.personLogg.etterspurteBehov(2.vedtaksperiode, Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering))
        inspektør.apply {
            assertTrue(
                utbetalingstidslinjer(1.vedtaksperiode)
                    .filterIsInstance<ArbeidsgiverperiodeDag>().isEmpty()
            )
        }
    }

    @Test
    fun `Forlengelse av søknad med utbetaling med opphold betalt i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(10.mai(2020), 2.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(10.mai(2020), 2.juni(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(10.mai(2020), 25.mai(2020))), førsteFraværsdag = 10.mai(2020))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.mai(2019) til 1.april(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(22.juni(2020), 11.juli(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.juni(2020), 21.juni(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 3.juni(2020), INNTEKT, true))
        )
        håndterYtelser(2.vedtaksperiode)
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
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
        assertTrue(person.personLogg.etterspurteBehov(1.vedtaksperiode, Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering))
        inspektør.apply {
            assertTrue(
                utbetalingstidslinjer(2.vedtaksperiode)
                    .filterIsInstance<ArbeidsgiverperiodeDag>().isEmpty()
            )
            assertTrue(
                utbetalingstidslinjer(1.vedtaksperiode)
                    .filterIsInstance<ArbeidsgiverperiodeDag>().isNotEmpty()
            )
        }
    }

    @Test
    fun `periode utvides ikke tilbake til arbeidsgiverperiode dersom det er gap mellom`() {
        håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(2.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 2.februar)
        val historikk = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, 1000.daglig))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar(2018), 1000.daglig, true))
        håndterYtelser(1.vedtaksperiode, *historikk, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
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
            AVSLUTTET
        )
        assertEquals(1, inspektør.utbetalinger.size)
        assertEquals(2.februar til 28.februar, inspektør.periode(1.vedtaksperiode))
        inspektør.utbetalinger.first().utbetalingstidslinje().also { utbetalingstidslinje ->
            assertAlleDager(utbetalingstidslinje, 1.januar til 16.januar, ArbeidsgiverperiodeDag::class)
            assertAlleDager(utbetalingstidslinje, 17.januar til 1.februar, UkjentDag::class, Arbeidsdag::class)
            assertAlleDager(utbetalingstidslinje, 2.februar til 28.februar, NavDag::class, NavHelgDag::class)
        }
    }

    private fun assertAlleDager(utbetalingstidslinje: Utbetalingstidslinje, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        utbetalingstidslinje.subset(periode).also { tidslinje ->
            assertTrue(tidslinje.all { it::class in dager }) {
                val ulikeDager = tidslinje.filter { it::class !in dager }
                "Forventet at alle dager skal være en av: ${dager.joinToString { it.simpleName ?: "UKJENT" }}.\n" +
                    ulikeDager.joinToString(prefix = "  - ", separator = "\n  - ", postfix = "\n") {
                        "${it.dato} er ${it::class.simpleName}"
                    } + "\nUtbetalingstidslinje:\n" + tidslinje.toString() + "\n"
            }
        }
    }

    @Test
    fun `maksdato blir riktig i ping-pong-perioder`() {
        val historikk1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 20.november(2019), 3.januar(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 4.januar(2020), 31.januar(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar(2020), 14.februar(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 15.februar(2020), 3.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 4.mars(2020), 20.mars(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 21.mars(2020), 17.april(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 18.april(2020), 8.mai(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 9.mai(2020), 29.mai(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning1 = listOf(
            Inntektsopplysning(ORGNUMMER, 20.november(2019), INNTEKT, true)
        )

        håndterSykmelding(Sykmeldingsperiode(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            *historikk1.toTypedArray(),
            inntektshistorikk = inntektsopplysning1,
            besvart = LocalDate.EPOCH.atStartOfDay()
        )
        håndterYtelser(
            1.vedtaksperiode,
            utbetalinger = historikk1.toTypedArray(),
            inntektshistorikk = inntektsopplysning1,
            besvart = LocalDate.EPOCH.atStartOfDay()
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        val historikk2 = historikk1 + listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 22.juni(2020), 9.juli(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juli(2020), 31.juli(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.august(2020), 17.august(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning2 = inntektsopplysning1 + listOf(
            Inntektsopplysning(ORGNUMMER, 22.juni(2020), INNTEKT, true)
        )

        håndterSykmelding(Sykmeldingsperiode(18.august(2020), 2.september(2020), 100.prosent))
        håndterSøknad(Sykdom(18.august(2020), 2.september(2020), 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historikk2.toTypedArray(), inntektshistorikk = inntektsopplysning2)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)


        håndterSykmelding(Sykmeldingsperiode(3.september(2020), 30.september(2020), 100.prosent))
        håndterSøknad(Sykdom(3.september(2020), 30.september(2020), 100.prosent))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(1.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(2.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(3.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(20.november(2019), inspektør.skjæringstidspunkt(3.vedtaksperiode))
    }

    @Test
    fun `Person uten refusjon til arbeidsgiver blir ikke behandlet i Spleis`() { // TODO
        // seeder personen med historisk refusjonsopplysning
        håndterInntektsmelding(listOf(7.oktober(2019) til 23.oktober(2019)), refusjon = Inntektsmelding.Refusjon(null, null))

        håndterSykmelding(Sykmeldingsperiode(23.oktober(2020), 18.november(2020), 100.prosent))
        håndterSøknad(Sykdom(23.oktober(2020), 18.november(2020), 100.prosent))
        val historikk = arrayOf(
            PersonUtbetalingsperiode(ORGNUMMER, 7.oktober(2019), 1.juli(2020), 100.prosent, 1000.daglig),
            Friperiode(2.juli(2020), 2.september(2020)),
            PersonUtbetalingsperiode(ORGNUMMER, 3.september(2020), 22.oktober(2020), 100.prosent, 1000.daglig)
        )
        val inntektsopplysning = listOf(
            Inntektsopplysning(ORGNUMMER, 7.oktober(2019), INNTEKT, false)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historikk, inntektshistorikk = inntektsopplysning)
        håndterYtelser(1.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `ping-pong hvor infotrygd perioden slutter på maksdato skal ikke føre til en automatisk annullering`() {
        val fom1 = 1.juni
        val tom1 = 30.juni
        håndterSykmelding(Sykmeldingsperiode(fom1, tom1, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(fom1, tom1, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.mai, 100.prosent, 1200.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1200.daglig, true, null))
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val fom2 = 13.desember
        val tom2 = 31.desember
        håndterSykmelding(Sykmeldingsperiode(fom2, tom2, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(fom2, tom2, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.mai, 100.prosent, 1200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.juli, 12.desember, 100.prosent, 1200.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1200.daglig, true, null),
                Inntektsopplysning(ORGNUMMER, 1.juli, 1200.daglig, true, null)
            )
        )
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
        assertEquals(Utbetaling.GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(1))
    }

    @Test
    fun `Ping-pong med ferie mellom Infotrygd-perioder skal ikke beregne nye skjæringstidspunkt etter ferien`() {
        håndterSykmelding(Sykmeldingsperiode(26.juni(2020), 26.juli(2020), 100.prosent))
        håndterSøknad(Sykdom(26.juni(2020), 26.juli(2020), 100.prosent))
        val historikk1 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juni(2020), 25.juni(2020), 100.prosent, 1200.daglig),
        )
        val inntektsopplysning1 = listOf(
            Inntektsopplysning(ORGNUMMER, 10.juni(2020), INNTEKT, true)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historikk1, inntektshistorikk = inntektsopplysning1)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(12.oktober(2020), 8.november(2020), 100.prosent))
        håndterSøknad(Sykdom(12.oktober(2020), 8.november(2020), 100.prosent))

        val historikk2 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juni(2020), 25.juni(2020), 60.prosent, 1200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 27.juli(2020), 6.september(2020), 60.prosent, 1200.daglig),
            Friperiode(7.september(2020), 8.september(2020)),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 9.september(2020), 13.september(2020), 60.prosent, 1200.daglig),
            Friperiode(14.september(2020), 15.september(2020)),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 16.september(2020), 11.oktober(2020), 60.prosent, 1200.daglig)
        )
        val inntektsopplysning2 = listOf(
            Inntektsopplysning(ORGNUMMER, 10.juni(2020), INNTEKT, true),
            Inntektsopplysning(ORGNUMMER, 27.juli(2020), INNTEKT, true)
        )
        håndterUtbetalingshistorikk(2.vedtaksperiode, *historikk2, inntektshistorikk = inntektsopplysning2)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(9.november(2020), 6.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(9.november(2020), 6.desember(2020), 100.prosent))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Sender perioden til Infotrygd hvis inntekt mangler ved bygging av utbetalingstidslinje`() {
        håndterSykmelding(Sykmeldingsperiode(26.juni(2020), 26.juli(2020), 60.prosent))
        håndterSøknad(Sykdom(26.juni(2020), 26.juli(2020), 60.prosent))
        val historikk = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.januar(2020), 25.januar(2020), 100.prosent, 1200.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 10.juni(2020), 25.juni(2020), 100.prosent, 1200.daglig)
        )
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 10.juni(2020), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *historikk, inntektshistorikk = inntektshistorikk)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Forlengelse oppdager ferie fra infotrygd`() {
        /* Vi ser at hvis vi oppdager ferie i infotrygd ender vi opp med ukjente dager i utbetalingstidslinja.
           Dette fører til en annullering i oppdraget. */
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 3.januar, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent), Ferie(16.februar, 17.februar))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk, besvart = LocalDateTime.now().minusHours(24))
        håndterYtelser(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk, besvart = LocalDateTime.now().minusHours(24))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertTilstander(
            1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET
        )

        håndterSykmelding(Sykmeldingsperiode(24.februar, 15.mars, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(24.februar, 15.mars, 100.prosent))
        håndterYtelser(2.vedtaksperiode, historikk, Friperiode(16.februar, 17.februar), inntektshistorikk = inntektshistorikk)
        håndterSimulering(2.vedtaksperiode)
        assertEquals(2, inspektør.arbeidsgiverOppdrag[1].linjerUtenOpphør().size)
    }

    @Test
    fun `infotrygd overtar periode med arbeidsgiverperiode 2`() {
        håndterSykmelding(Sykmeldingsperiode(22.januar, 28.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 6.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 6.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(7.februar, 7.februar, 100.prosent))
        håndterSøknad(Sykdom(7.februar, 7.februar, 100.prosent))
        person.søppelbøtte(hendelselogg, 7.februar til 7.februar) // perioden fikk en error som trigget utkastelse
        håndterSykmelding(Sykmeldingsperiode(8.februar, 25.februar, 100.prosent))
        håndterSøknad(Sykdom(8.februar, 25.februar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 7.februar, 7.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 7.februar, INNTEKT, true)
        ))
        håndterYtelser(3.vedtaksperiode)
        val utbetaling = inspektør.utbetaling(0).inspektør
        assertEquals(8.februar til 25.februar, utbetaling.periode)
        assertTrue(utbetaling.utbetalingstidslinje[8.februar] is NavDag)
    }

    @Test
    fun `infotrygd overtar periode med arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 18.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 18.januar, 100.prosent))
        person.søppelbøtte(hendelselogg, 11.januar til 18.januar) // perioden fikk en error som trigget utkastelse
        håndterSykmelding(Sykmeldingsperiode(19.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(19.januar, 25.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 18.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
        ))
        håndterYtelser(3.vedtaksperiode)
        val utbetaling = inspektør.utbetaling(0).inspektør
        assertEquals(19.januar til 25.januar, utbetaling.periode)
        assertTrue(utbetaling.utbetalingstidslinje[19.januar] is NavDag)
    }

    @Test
    fun `infotrygd overtar periode med arbeidsgiverperiode med opphold`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 22.januar, 100.prosent))
        person.søppelbøtte(hendelselogg, 18.januar til 22.januar) // perioden fikk en error som trigget utkastelse
        håndterSykmelding(Sykmeldingsperiode(23.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(23.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 22.januar, 22.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 22.januar, INNTEKT, true)
        ))
        håndterYtelser(3.vedtaksperiode)
        val utbetaling = inspektør.utbetaling(0).inspektør
        assertEquals(23.januar til 31.januar, utbetaling.periode)
        assertTrue(utbetaling.utbetalingstidslinje[23.januar] is NavDag)
    }

    @Test
    fun `infotrygd forlengelse med skjæringstidspunkt vi allerede har vilkårsvurdert burde bruke vilkårsgrunnlag fra infotrygd`() {
        tilGodkjenning(fom = 1.januar, tom = 31.januar, grad = 100.prosent, førsteFraværsdag = 1.januar)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertInstanceOf(VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag::class.java, person.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `IT forlengelse hvor arbeidsgiver har endret orgnummer og vi har fått nye inntektsopplysninger fra IT ved skjæringstidspunktet`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(a1, 1.januar, INNTEKT, true)),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        val vilkårsgrunnlag = person.vilkårsgrunnlagFor(1.januar)
        håndterAnnullerUtbetaling(a1, inspektør.fagsystemId(1.vedtaksperiode))
        håndterUtbetalt(orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a2, 1.januar, 31.januar, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 1.februar, 28.februar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(a2, 1.januar, INNTEKT, true)),
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertNotSame(vilkårsgrunnlag, person.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `overskriver ikke spleis vilkårsgrunnlag pga inntekt fra IT dersom vi allerede har en utbetalt periode i spleis`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(a1, 1.januar, INNTEKT, true)),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        val vilkårsgrunnlag = person.vilkårsgrunnlagFor(1.januar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a2, 1.januar, 31.januar, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(a2, 1.februar, 28.februar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(a2, 1.januar, INNTEKT, true)),
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertSame(vilkårsgrunnlag, person.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `lagrer ikke inntekt fra infotrygd uten utbetaling som vilkårsgrunnlag i spleis`() {
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = inntektshistorikk)
        håndterInntektsmelding(listOf(1.januar til 31.januar))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = inntektshistorikk)
        assertForventetFeil(
            forklaring = "https://trello.com/c/MBCGez52",
            nå = { assertNotNull(person.vilkårsgrunnlagFor(1.januar)) },
            ønsket = { assertNull(person.vilkårsgrunnlagFor(1.januar)) }
        )
    }

    @Test
    fun `forlengelse fra IT skal ikke kobles med periode i AvsluttetUtenUtbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        person.invaliderAllePerioder(Aktivitetslogg(), null)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
        ))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))
        assertEquals(Periodetype.OVERGANG_FRA_IT, inspektør.periodetype(3.vedtaksperiode))
    }

    @Test
    fun `forlengelse fra infotrygd hvor arbeidsgiverdager blir igjen fra IM etter tidligere forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent))
        håndterInntektsmelding(listOf(9.januar til 24.januar))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        /*
        Dette vil kaste ut den første perioden, men siden vi har en periode etter tom-datoen til denne perioden vil beholde arbeidsgiverperiodedager fra IM
        Perioden overlapper med en dag(22.januar), dagene 23. og 24. januar blir igjen fra IM i arbeidsgiverens sykdomstidslinje
         */
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))

        håndterInntektsmelding(listOf(1.februar til 16.februar))

        // Perioden i infotrygd trenger ikke nødvendigvis å tilstøte, men det _må_ være arbeidsgiverperiodedager før første utbetalte dag i IT
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 25.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 25.januar, INNTEKT, true))
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertEquals(9.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(Periodetype.OVERGANG_FRA_IT, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `førstegangsbehandling skal ikke hoppe videre dersom det kun er inntekt i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
        ))
        håndterSimulering(1.vedtaksperiode)

        assertForventetFeil(
            "vet ikke om skjæringstidspunktet blir feil, men vedtaksperioden burde uansett ikke markeres som førstegangsbehandling når det ikke er gjort vilkårsprøving",
            nå = {
                assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
                assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, inspektør.periodetype(1.vedtaksperiode))
            },
            ønsket = {
                assertEquals(17.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
                assertEquals(Periodetype.OVERGANG_FRA_IT, inspektør.periodetype(1.vedtaksperiode))
            }
        )
    }
}
