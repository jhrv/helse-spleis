package no.nav.helse.spleis.e2e

import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import org.junit.jupiter.api.Test

internal class RevurderingV2E2ETest : AbstractEndToEndTest() {

    @Test
    fun `tilstandsendringer i ny revurderingsflyt`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))

        assertForventetFeil(
            forklaring = "Planlagt flyt for ny revurdering",
            ønsket = {
                assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
                assertTilstander(2.vedtaksperiode, START, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
                assertTilstander(3.vedtaksperiode, START, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            },
            nå = {
                assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET, AVVENTER_HISTORIKK_REVURDERING)
                assertTilstander(2.vedtaksperiode, START, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET, AVVENTER_ARBEIDSGIVERE_REVURDERING)
                assertTilstander(3.vedtaksperiode, START, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET, AVVENTER_ARBEIDSGIVERE_REVURDERING)
            }
        )
    }
}