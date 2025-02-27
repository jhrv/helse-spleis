package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.Test

internal class HistorieFlereArbeidsgivereTest : HistorieTest() {

    @Test
    fun `infotrygd ag1 - spleis ag 2 - spleis ag 1`() {
        historie(utbetaling(17.januar, 31.januar, orgnr = AG1))
        addSykdomshistorikk(AG2, sykedager(1.februar, 28.februar))
        addSykdomshistorikk(AG1, sykedager(1.mars, 31.mars))
        val utbetalingstidslinjeAG1 = beregn(AG1, 17.januar)
        assertSkjæringstidspunkt(utbetalingstidslinjeAG1, 1.mars til 31.mars, 17.januar)
        assertAlleDager(utbetalingstidslinjeAG1, 1.mars til 16.mars, ArbeidsgiverperiodeDag::class)
        assertAlleDager(utbetalingstidslinjeAG1, 17.mars til 31.mars, NavDag::class, NavHelgDag::class)

        val utbetalingstidslinjeAG2 = beregn(AG2, 17.januar)
        assertSkjæringstidspunkt(utbetalingstidslinjeAG2, 1.februar til 28.februar, 17.januar)
        assertAlleDager(utbetalingstidslinjeAG2, 1.februar til 16.februar, ArbeidsgiverperiodeDag::class)
        assertAlleDager(utbetalingstidslinjeAG2, 17.februar til 28.februar, NavDag::class, NavHelgDag::class)
    }
}
