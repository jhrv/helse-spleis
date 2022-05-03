package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.økonomi.betal
import no.nav.helse.økonomi.er6GBegrenset
import java.time.LocalDate

internal object MaksimumUtbetaling {

    private var harRedusertUtbetaling = false

    internal fun betal(
        tidslinjer: List<Utbetalingstidslinje>,
        aktivitetslogg: IAktivitetslogg,
        virkningsdato: LocalDate
    ) {
        Utbetalingstidslinje.periode(tidslinjer).forEach { dato ->
            tidslinjer.map { it[dato].økonomi }.also { økonomiList ->
                try {
                    økonomiList.betal(virkningsdato)
                    harRedusertUtbetaling = harRedusertUtbetaling || økonomiList.er6GBegrenset()
                } catch (err: Exception) {
                    throw IllegalArgumentException("Klarte ikke å utbetale for dag=$dato, fordi: ${err.message}", err)
                }
            }
        }
        if (harRedusertUtbetaling)
            aktivitetslogg.info("Redusert utbetaling minst én dag på grunn av inntekt over 6G")
        else
            aktivitetslogg.info("Utbetaling har ikke blitt redusert på grunn av 6G")
    }
}
