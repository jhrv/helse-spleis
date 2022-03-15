package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.økonomi.Økonomi

internal interface ArbeidsgiverperiodeMediator {
    fun fridag(dato: LocalDate)
    fun arbeidsdag(dato: LocalDate)
    fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi)
    fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi)
    fun ukjentdag(dato: LocalDate, økonomi: Økonomi) = utbetalingsdag(dato, økonomi)
    fun foreldetDag(dato: LocalDate, økonomi: Økonomi)
    fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse)
    fun arbeidsgiverperiodeAvbrutt() {}
    fun arbeidsgiverperiodeFerdig() {}
    fun arbeidsgiverperiodeSistedag() {}
    fun oppholdsdag() {}
}
