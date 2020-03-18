package no.nav.helse.bugs_showstoppers

import no.nav.helse.e2e.AbstractEndToEndTest
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Periode.Egenmelding
import no.nav.helse.hendelser.Søknad.Periode.Sykdom
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class E2EEpic3Test : AbstractEndToEndTest() {

    @Test
    internal fun `forlenger ikke vedtaksperiode som har gått til infotrygd`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterPåminnelse(0, MOTTATT_SYKMELDING_FERDIG_GAP)
        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(1, Sykdom(29.januar, 23.februar, 100))
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(1, START, TIL_INFOTRYGD)
    }

    @Test
    internal fun `gradert sykmelding først`() {
        // ugyldig sykmelding lager en tom vedtaksperiode uten tidslinje, som overlapper med alt
        håndterSykmelding(Triple(3.januar(2020), 3.januar(2020), 50))
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        håndterSykmelding(Triple(13.januar(2020), 17.januar(2020), 100))
        håndterSøknad(Sykdom(13.januar(2020), 17.januar(2020), 100))
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP)
    }

    @Test
    internal fun `Søknad treffer flere perioder`() {
        håndterSykmelding(Triple(1.januar(2020), 5.januar(2020), 100))
        håndterSykmelding(Triple(6.januar(2020), 10.januar(2020), 100))
        håndterSykmelding(Triple(13.januar(2020), 17.januar(2020), 100))
        håndterSøknad(
            Sykdom(13.januar(2020), 17.januar(2020), 100),
            Egenmelding(30.desember(2019), 31.desember(2019))
        )
        håndterSykmelding(Triple(18.januar(2020), 26.januar(2020), 100))
        håndterSøknad(Sykdom(18.januar(2020), 26.januar(2020), 100))
        håndterSykmelding(Triple(27.januar(2020), 30.januar(2020), 100))
        håndterSøknad(Sykdom(27.januar(2020), 30.januar(2020), 100))
        håndterSykmelding(Triple(30.januar(2020), 14.februar(2020), 100))
        håndterSykmelding(Triple(30.januar(2020), 14.februar(2020), 100))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(30.desember(2019), 31.desember(2019)),
                Periode(1.januar(2020), 5.januar(2020)),
                Periode(6.januar(2020), 10.januar(2020)),
                Periode(13.januar(2020), 16.januar(2020))
            ), førsteFraværsdag = 13.januar(2020)
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(30.desember(2019), 31.desember(2019)),
                Periode(1.januar(2020), 5.januar(2020)),
                Periode(6.januar(2020), 10.januar(2020)),
                Periode(13.januar(2020), 16.januar(2020))
            ), førsteFraværsdag = 13.januar(2020)
        )

        assertTilstander(0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP
        )
        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE
        )
        assertTilstander(
            2,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE
        )
        assertTilstander(3,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        assertTilstander(4,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
        Assertions.assertEquals(5, observatør.tilstander.size)
    }

    @Test
    internal fun `Ingen sykedager i tidslinjen - første fraværsdag bug`() {
        håndterSykmelding(Triple(6.januar(2020), 7.januar(2020), 100))
        håndterSykmelding(Triple(8.januar(2020), 10.januar(2020), 100))
        håndterSykmelding(Triple(27.januar(2020), 28.januar(2020), 100))

        håndterInntektsmelding(
            listOf(
                Periode(18.november(2019), 23.november(2019)),
                Periode(14.oktober(2019), 18.oktober(2019)),
                Periode(1.november(2019), 5.november(2019))
            ), 18.november(2019), listOf(
                Periode(5.desember(2019), 6.desember(2019)),
                Periode(30.desember(2019), 30.desember(2019)),
                Periode(2.januar(2020), 3.januar(2020)),
                Periode(22.januar(2020), 22.januar(2020))
            )
        )

        assertTilstander(0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP
        )
        assertTilstander(1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE
        )
        assertTilstander(2, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    internal fun `inntektsmelding starter etter sykmeldingsperioden`() {
        håndterSykmelding(Triple(15.januar(2020), 12.februar(2020), 100))
        håndterSøknad(Sykdom(15.januar(2020), 12.februar(2020), 100))
        håndterInntektsmelding(listOf(Periode(16.januar(2020), 31.januar(2020))), 16.januar(2020))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(
            0,
            Triple(3.april(2019), 30.april(2019), 100),
            Triple(18.mars(2018), 2.april(2018), 100),
            Triple(29.november(2017), 3.desember(2017), 100),
            Triple(13.november(2017), 28.november(2017), 100)
        )
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    internal fun `periode uten sykedager`() {
        håndterSykmelding(Triple(3.januar, 4.januar, 100))
        håndterSykmelding(Triple(8.januar, 9.januar, 100))
        håndterSykmelding(Triple(15.januar, 16.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(
            Periode(3.januar, 4.januar),
            Periode(15.januar, 16.januar)))

        håndterSøknadMedValidering(0, Sykdom(3.januar, 4.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterManuellSaksbehandling(0, true)

        håndterSøknadMedValidering(1, Sykdom(8.januar, 9.januar, 100))
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history
        håndterManuellSaksbehandling(1, true)

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
        }
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )

        assertTilstander(
            2,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP
        )
    }

    @Test
    internal fun `enkeltstående sykedag i arbeidsgiverperiode-gap`() {
        håndterSykmelding(Triple(10.februar(2020), 12.februar(2020), 100))
        håndterSykmelding(Triple(14.februar(2020), 14.februar(2020), 100))
        håndterSykmelding(Triple(27.februar(2020), 28.februar(2020), 100))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(10.februar(2020), 12.februar(2020)),
                Periode(27.februar(2020), 28.februar(2020))
            ),
            førsteFraværsdag = 27.februar(2020)
        )
        assertTilstander(0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP
        )
        assertTilstander(1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP
        )
        assertTilstander(2,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP
        )
    }

    @Test
    internal fun `Inntektsmelding med ferie etter arbeidsgiverperioden`() {
        håndterSykmelding(Triple(10.januar(2020), 21.januar(2020), 100))
        håndterSykmelding(Triple(23.januar(2020), 24.januar(2020), 100))
        håndterSøknad(Sykdom(23.januar(2020), 24.januar(2020), 100))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(6.januar(2020), 21.januar(2020))),
            førsteFraværsdag = 23.januar(2020),
            ferieperioder = listOf(Periode(4.februar(2020), 5.februar(2020)))
        )

        assertTilstander(
            0,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP
        )
        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_UFERDIG_GAP
        )
    }

    @Test
    internal fun `ignorerer egenmeldingsdag i søknaden langt tilbake i tid`() {
        håndterSykmelding(Triple(6.januar(2020), 23.januar(2020), 100))
        håndterSøknad(
            Egenmelding(24.september(2019), 24.september(2019)), // ignored because it's too long ago relative to 6.januar
            Sykdom(6.januar(2020), 23.januar(2020), 100)
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(24.september(2019), 24.september(2019)),
                Periode(27.september(2019), 6.oktober(2019)),
                Periode(14.oktober(2019), 18.oktober(2019))
            ),
            førsteFraværsdag = 24.september(2019),
            ferieperioder = listOf(Periode(7.oktober(2019), 11.oktober(2019)))
        )
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP)
    }

    @Test
    internal fun `person med gammel sykmelding`() {
        håndterSykmelding(Triple(13.januar(2020), 31.januar(2020), 100))
        håndterSykmelding(Triple(9.februar(2017), 15.februar(2017), 100))
        håndterSøknad(Sykdom(13.januar(2020), 31.januar(2020), 100))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(13.januar(2020), 21.januar(2020))
            ),
            førsteFraværsdag = 13.januar(2020)
        )
        håndterVilkårsgrunnlag(1, INNTEKT)
        håndterYtelser(1)   // No history

        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING)
    }
}

