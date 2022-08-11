package no.nav.helse.spleis.e2e

import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import org.junit.jupiter.api.Test

internal class OverstyrUtkastTilRevurderingFlereAGTest : AbstractEndToEndTest() {

    private companion object {
        private val AG1 = "123456789"
        private val AG2 = "987612345"
    }

    @Test
    fun `overstyr utkast til revurdering flere ag - første ag mottar overstyring og går helt til utbetalt`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2)
        forlengVedtak(1.februar, 28.februar, AG1, AG2)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(2.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(2.vedtaksperiode, orgnummer = AG1)

        håndterOverstyrTidslinje((30.januar til 31.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(2.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(orgnummer = AG1)

        håndterYtelser(2.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = AG2)

        inspektør(AG1) {
            val utbetalingstidslinje = sisteUtbetalingUtbetalingstidslinje()
            assertUtbetalingsdag(utbetalingstidslinje[29.januar], expectedDagtype = Fridag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[30.januar], expectedDagtype = Fridag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[31.januar], expectedDagtype = Fridag::class, 50.0)
            assertNoErrors()
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }

        inspektør(AG2) {
            val utbetalingstidslinje = sisteUtbetalingUtbetalingstidslinje()
            assertNoErrors()
            assertUtbetalingsdag(utbetalingstidslinje[29.januar], expectedDagtype = NavDag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[30.januar], expectedDagtype = NavDag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[31.januar], expectedDagtype = NavDag::class, 50.0)

            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `overstyr utkast til revurdering flere ag - første ag mottar overstyring`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2)
        forlengVedtak(1.februar, 28.februar, AG1, AG2)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(2.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(2.vedtaksperiode, orgnummer = AG1)

        håndterOverstyrTidslinje((30.januar til 31.januar).map { manuellFeriedag(it) }, orgnummer = AG1)

        inspektør(AG1) {
            assertNoErrors()
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING
            )
        }

        inspektør(AG2) {
            assertNoErrors()
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `overstyr utkast til revurdering flere ag - andre ag mottar overstyring`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2)
        forlengVedtak(1.februar, 28.februar, AG1, AG2)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(2.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(2.vedtaksperiode, orgnummer = AG1)

        håndterOverstyrTidslinje((30.januar til 31.januar).map { manuellFeriedag(it) }, orgnummer = AG2)
        håndterYtelser(2.vedtaksperiode, orgnummer = AG1)

        inspektør(AG1) {
            val utbetalingstidslinje = sisteUtbetalingUtbetalingstidslinje()
            assertUtbetalingsdag(utbetalingstidslinje[29.januar], expectedDagtype = Fridag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[30.januar], expectedDagtype = NavDag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[31.januar], expectedDagtype = NavDag::class, 50.0)
            assertNoErrors()
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        }

        inspektør(AG2) {
            val utbetalingstidslinje = sisteUtbetalingUtbetalingstidslinje()
            assertUtbetalingsdag(utbetalingstidslinje[29.januar], expectedDagtype = NavDag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[30.januar], expectedDagtype = Fridag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[31.januar], expectedDagtype = Fridag::class, 50.0)
            assertNoErrors()
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `overstyr utkast til revurdering flere ag - andre ag mottar overstyring i siste periode`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2)
        forlengVedtak(1.februar, 28.februar, AG1, AG2)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(2.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(2.vedtaksperiode, orgnummer = AG1)

        håndterOverstyrTidslinje((1.februar til 2.februar).map { manuellFeriedag(it) }, orgnummer = AG2)
        håndterYtelser(2.vedtaksperiode, orgnummer = AG1)

        inspektør(AG1) {
            val utbetalingstidslinje = sisteUtbetalingUtbetalingstidslinje()
            assertUtbetalingsdag(utbetalingstidslinje[29.januar], expectedDagtype = Fridag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[30.januar], expectedDagtype = Fridag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[31.januar], expectedDagtype = Fridag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[1.februar], expectedDagtype = NavDag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[2.februar], expectedDagtype = NavDag::class, 50.0)

            assertNoErrors()
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        }

        inspektør(AG2) {
            val utbetalingstidslinje = sisteUtbetalingUtbetalingstidslinje()
            assertUtbetalingsdag(utbetalingstidslinje[29.januar], expectedDagtype = NavDag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[30.januar], expectedDagtype = NavDag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[31.januar], expectedDagtype = NavDag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[1.februar], expectedDagtype = Fridag::class, 50.0)
            assertUtbetalingsdag(utbetalingstidslinje[2.februar], expectedDagtype = Fridag::class, 50.0)

            assertNoErrors()
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `overstyr utkast til revurdering flere ag - kan ikke overstyre periode i én ag dersom annen ag er revurdert ferdig`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2)
        forlengVedtak(1.februar, 28.februar, AG1, AG2)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(2.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(orgnummer = AG1)

        håndterOverstyrTidslinje((1.februar til 2.februar).map { manuellFeriedag(it) }, orgnummer = AG2)
        inspektør(AG1) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }
        inspektør(AG2) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING
            )
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING
            )
        }
    }

    @Test
    fun `revurder revurdering flere ag - kan ikke revurdere én ag hvis en annen ag ikke er ferdig revurdert`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2)
        forlengVedtak(1.februar, 28.februar, AG1, AG2)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(2.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(orgnummer = AG1)

        håndterOverstyrTidslinje((1.februar til 2.februar).map { manuellFeriedag(it) }, orgnummer = AG1)
        inspektør(AG1) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING
            )
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING
            )
        }
        inspektør(AG2) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_REVURDERING
            )
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_REVURDERING
            )
        }
    }
}
