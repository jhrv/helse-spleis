package no.nav.helse.bugs_showstoppers

import java.time.LocalDate
import no.nav.helse.etterspurteBehov
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.ArbeidsforholdV2
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSammenligningsgrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Medlemskap
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ManglendeVilkårsgrunnlagTest : AbstractEndToEndTest() {

    @Test
    fun `inntektsmelding avslutter to korte perioder og flytter nav-perioden uten å utføre vilkårsprøving`() {
        håndterSykmelding(Sykmeldingsperiode(9.januar, 15.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(19.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(19.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 2.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 2.februar, 100.prosent))
        // inntektsmeldingen lukker de to korte periodene og gjør samtidig at
        // nav-perioden går fra AvventerInntektsmeldingUferdigForlengelse til AvventerUferdigForlengelse.
        // Når Inntektsmeldingen er håndtert sendes GjenopptaBehandling ut, som medfører at
        // NAV-perioden går videre til AvventerHistorikk uten å gjøre vilkårsvurdering først
        håndterInntektsmelding(
            listOf(
                9.januar til 15.januar,
                19.januar til 26.januar,
                29.januar til 29.januar
            ), 29.januar
        )
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `inntektsmelding drar periode tilbake og lager tilstøtende`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(26.januar, 2.februar, 100.prosent))
        håndterSøknad(Sykdom(26.januar, 2.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(5.februar, 21.februar, 100.prosent))
        håndterSøknad(Sykdom(5.februar, 21.februar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        // inntektsmelding inneholder en ukjent dag — 8. januar — som vi ikke
        // har registrert før i forbindelse med verken søknad eller sykmelding
        // Dette medfører at perioden 26. januar - 2. februar "dras tilbake"
        // til 6. januar, siden 8. januar er en mandag og 5. januar (forrige agp-innslag) er en fredag
        // så regnes lørdag + søndag som del av arbeidsgiverperioden også.
        // Dermed ble perioden 6. januar - 2. februar regnet som tilstøtende til 1.-5. januar, selv om
        // de to har forskjellige skjæringstidspunkt.
        håndterInntektsmelding(
            listOf(
                1.januar til 5.januar,
                8.januar til 8.januar,
                24.januar til 2.februar
            ), 24.januar
        )

        håndterYtelser(3.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING
        )

        assertEquals(1, person.personLogg.etterspurteBehov(1.vedtaksperiode).size)
        assertEquals(1, person.personLogg.etterspurteBehov(2.vedtaksperiode).size)
        assertTrue(person.personLogg.etterspurteBehov(3.vedtaksperiode).map { it.type }
            .containsAll(listOf(InntekterForSammenligningsgrunnlag, Medlemskap, InntekterForSykepengegrunnlag, ArbeidsforholdV2)))
    }
}
