package no.nav.helse.hendelser

import no.nav.helse.hendelser.Periode.Companion.slutterEtter
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate

class Arbeidsavklaringspenger(private val perioder: List<Periode>) {
    internal fun valider(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate): IAktivitetslogg {
        if (perioder.slutterEtter(skjæringstidspunkt.minusMonths(6))) {
            aktivitetslogg.varsel("Bruker har mottatt AAP innenfor 6 måneder før skjæringstidspunktet. Kontroller at brukeren har rett til sykepenger")
        }
        return aktivitetslogg
    }
}
