package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvis
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager

internal class AvvisDagerEtterDødsdatofilter(
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val dødsdato: LocalDate?,
    private val aktivitetslogg: IAktivitetslogg
) {
    private val avvisFra = dødsdato?.plusDays(1) ?: LocalDate.MAX
    internal fun filter(): List<Utbetalingstidslinje> {
        if (dødsdato == null || avvisFra !in periode) return tidslinjer
        return avvis(tidslinjer, listOf(avvisFra til LocalDate.MAX), listOf(Begrunnelse.EtterDødsdato)).also { result ->
            if (avvisteDager(result, periode, Begrunnelse.EtterDødsdato).isNotEmpty())
                aktivitetslogg.info("Utbetaling stoppet etter $dødsdato grunnet dødsfall")
            else
                aktivitetslogg.info("Personen døde $dødsdato, utenfor den aktuelle perioden.")
        }
    }

}
