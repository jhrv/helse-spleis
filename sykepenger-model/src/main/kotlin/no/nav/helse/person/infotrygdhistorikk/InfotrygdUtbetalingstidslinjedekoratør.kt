package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeMediator
import no.nav.helse.økonomi.Økonomi

internal class InfotrygdUtbetalingstidslinjedekoratør(
    private val other: ArbeidsgiverperiodeMediator,
    private val utbetaltePerioder: List<Periode>
) : ArbeidsgiverperiodeMediator by(other) {
    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi) {
        if (utbetaltePerioder.any { dato in it }) return other.ukjentdag(dato, økonomi)
        other.utbetalingsdag(dato, økonomi)
    }
}
