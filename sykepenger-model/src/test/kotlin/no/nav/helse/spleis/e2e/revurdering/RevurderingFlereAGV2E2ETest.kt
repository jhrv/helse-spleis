package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDateTime
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.Dagtype.Sykedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.forlengelseTilGodkjenning
import no.nav.helse.spleis.e2e.førstegangTilGodkjenning
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype.REVURDERING
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RevurderingFlereAGV2E2ETest: AbstractEndToEndTest() {

    @Test
    fun `revurdere første periode - flere ag - ag 1`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), orgnummer = a1)
        håndterYtelser(3.vedtaksperiode, orgnummer = a1)
        håndterSimulering(3.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere første periode - flere ag - ag 1 - har generert utbetaling`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), orgnummer = a1)
        håndterYtelser(3.vedtaksperiode, orgnummer = a1)
        håndterSimulering(3.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)

        inspektør(a1) {
            assertEquals(REVURDERING, utbetalingstype(3.vedtaksperiode, 1))
            assertEquals(4, utbetalinger.size)
        }

        inspektør(a2) {
            assertEquals(REVURDERING, utbetalingstype(3.vedtaksperiode, 2))
        }

        assertForventetFeil(
            forklaring = "Utbetalinger må være genreret før noe går til godkjenning fordi er avhengig av utbetalinger for å lage generasjoner",
            nå = {
                inspektør(a2) {
                    assertEquals(7, utbetalinger.size)
                }
            },
            ønsket = {
                inspektør(a2) {
                    assertEquals(4, utbetalinger.size)
                }
            }
        )
    }


    @Test
    fun `revurdere andre periode - flere ag - ag 1`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)), orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere tredje periode - flere ag - ag1`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag)), orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere første periode - flere ag - ag 2`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere andre periode - flere ag - ag 2`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere tredje periode - flere ag - ag2`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag)), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `starte revurdering av ag1 igjen etter at ag2 har startet revurdering`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdering påvirkes ikke av gjenoppta behandling ved avsluttet uten utbetaling`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        nullstillTilstandsendringer()
        håndterSykmelding(Sykmeldingsperiode(1.juni, 10.juni, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.juni, 10.juni, 100.prosent), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
    }

    @Test
    fun `revurdering av ag 2 mens ag 1 er til utbetaling`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengelseTilGodkjenning(1.februar, 28.februar, a1, a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a2)
        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING)
        }

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        nullstillTilstandsendringer()
        håndterUtbetalt()

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        nullstillTilstandsendringer()
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        }

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }

        nullstillTilstandsendringer()
        håndterYtelser(2.vedtaksperiode, orgnummer = a2)

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `revurdering av tidligere frittstående periode hos ag3 mens overlappende hos ag1 og ag2 utbetales`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a3)

        nyeVedtak(1.mai, 31.mai, a1, a2)
        forlengelseTilGodkjenning(1.juni, 30.juni, a1, a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a3)
        håndterUtbetalt(orgnummer = a1)

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
        }
        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        inspektør(a3) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        inspektør(a3) {
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        }
    }

    @Test
    fun `revurdering av senere frittstående periode hos ag3 mens overlappende out of order hos ag1 og ag2 utbetales`() {
        nyttVedtak(1.april, 30.april, orgnummer = a3)

        nyeVedtak(1.februar, 28.februar, a1, a2)
        forlengelseTilGodkjenning(1.mars, 15.mars, a1, a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        assertForventetFeil(
            nå = {
                assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a3)
            }
        )

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.april, Feriedag)), a3)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a3)

        håndterUtbetalt(orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a3)

        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET, orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a3)
    }

    @Test
    fun `revurdering av senere frittstående periode hos ag3 mens overlappende forlengelse out of order hos ag1 og ag2 utbetales`() {
        nyttVedtak(1.april, 30.april, orgnummer = a3)
        tilGodkjenning(1.februar, 28.februar, a1, a2)
        nullstillTilstandsendringer()
        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        inspektør(a3) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
        }
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        }
        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
        assertForventetFeil(
            forklaring = "Når en periode avsluttes bør det trigge revurdering av alle senere avsluttede perioder",
            nå = {
                inspektør(a3) {
                    assertTilstander(1.vedtaksperiode, AVSLUTTET)
                }
            },
            ønsket = {
                inspektør(a3) {
                    assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
                }
            }
        )
    }

    @Test
    fun `revurdering av ag 1 kicker i gang revurdering av ag 2 - holder igjen senere perioder hos ag1`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)

        nyttVedtak(1.april, 30. april, orgnummer = a1)

        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt for ag 1 mens senere periode for ag 1 er til utbetaling`() {
        /* Rekkefølge ting burde skje i:
        * 1. a1 v2 utbetales, a1 v1 avventer revurdering, a2 v1 avventer andre arbeidsgivere
        * 2. a1 v2 utbetalt, a1 v1 revurderes, a2 v1 avventer andre arbeidsgivere
        * 3. a1 v1 revurdert, a1 v2 revurderes, a2 v1 avventer andre arbeidsgivere
        * */
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        førstegangTilGodkjenning(1.mars, 31.mars, a1, a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)

        nullstillTilstandsendringer()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING)
        }

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        håndterUtbetalt(orgnummer = a1)

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)
        }

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `tre ag der a1 og a3 har to førstegangsbehandlinger - første førstegang på a1 blir revurdert mens andre førstegang på a1 er til utbetaling`() {
        nyeVedtak(1.januar, 31.januar, a1, a3)
        førstegangTilGodkjenning(1.mars, 31.mars, a1, a2, a3)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)

        nullstillTilstandsendringer()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING)
        }

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        inspektør(a3) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        håndterUtbetalt(orgnummer = a1)

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)
        }

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        inspektør(a3) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `revurdering av ag 2 mens ag 1 revurderes og er til utbetaling`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a2)

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING)
        }

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        håndterUtbetalt(orgnummer = a1)

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
        }

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }

        nullstillTilstandsendringer()
        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
        }

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        }
    }

    @Test
    fun `validerer ytelser på alle arbeidsgivere ved revurdering av flere arbeidgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)

        håndterYtelser(1.vedtaksperiode, foreldrepenger = 20.januar til 31.januar, orgnummer = a1)
        assertVarsel("Det er utbetalt foreldrepenger i samme periode.", 1.vedtaksperiode.filter(a1))
        assertVarsel("Det er utbetalt foreldrepenger i samme periode.", 1.vedtaksperiode.filter(a2))
    }

    @Test
    fun `validerer ytelser for periode som strekker seg fra skjæringstidsunkt til siste tom`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a2)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)
        håndterYtelser(2.vedtaksperiode, foreldrepenger = 20.mars til 31.mars, orgnummer = a1)

        assertVarsel("Det er utbetalt foreldrepenger i samme periode.", 3.vedtaksperiode.filter(a2))
    }

    @Test
    fun `forkaster gamle utbetalinger for flere AG`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(18.januar, Feriedag)), a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        inspektør(a2).utbetalinger(2.vedtaksperiode).also { utbetalinger ->
            assertEquals(1, utbetalinger.filter { it.inspektør.erUbetalt }.size)
        }
        inspektør(a2).utbetalinger(1.vedtaksperiode).also { utbetalinger ->
            assertEquals(1, utbetalinger.filter { it.inspektør.erUbetalt }.size)
        }
    }

    @Test
    fun `ag1 har en kortere periode enn ag2 - ag1 revurderes - ag2 sin utbetaling blir ikke kuttet`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a2)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        inspektør(a2).gjeldendeUtbetalingForVedtaksperiode(3.vedtaksperiode).also {
            assertEquals(17.januar til 31.mars, it.inspektør.periode)
        }
    }

    @Test
    fun `Varsel på perioder hos begge AG dersom grad er under 20 prosent`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Sykedag, 19)), orgnummer = a1)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Sykedag, 19)), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        assertEquals(19, inspektør(a1).sykdomstidslinje.inspektør.grader[17.januar])
        assertEquals(19, inspektør(a2).sykdomstidslinje.inspektør.grader[17.januar])
        assertVarsel("Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %. Vurder å sende vedtaksbrev fra Infotrygd", 1.vedtaksperiode.filter(a2))
        assertVarsel("Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %. Vurder å sende vedtaksbrev fra Infotrygd", 1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `revurdere på en eldre arbeidsgiver - infotrygd har utbetalt `() {
        nyeVedtak(1.januar, 31.januar, a1)
        nyPeriode(1.februar til 18.februar, a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2, besvart = LocalDateTime.now().minusHours(48))

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), orgnummer = a1)

        nullstillTilstandsendringer()

        håndterYtelser(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(a2, 17.januar, 18.februar, 100.prosent, INNTEKT), orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, orgnummer = a1)
    }
}