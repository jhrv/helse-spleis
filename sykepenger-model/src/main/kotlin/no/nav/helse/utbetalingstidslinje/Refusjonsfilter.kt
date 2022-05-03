package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk

internal object Refusjonsfilter {
    internal fun filter(
        arbeidsgivere: Map<Arbeidsgiver, Utbetalingstidslinje>,
        infotrygdhistorikk: Infotrygdhistorikk,
        aktivitetslogg: IAktivitetslogg,
        periode: Periode
    ) {
        arbeidsgivere.forEach { (arbeidsgiver, tidslinje) ->
            Refusjonsgjødsler(
                tidslinje = tidslinje + arbeidsgiver.utbetalingstidslinje(infotrygdhistorikk),
                refusjonshistorikk = arbeidsgiver.refusjonshistorikk,
                infotrygdhistorikk = infotrygdhistorikk,
                organisasjonsnummer = arbeidsgiver.organisasjonsnummer()
            ).gjødsle(aktivitetslogg, periode)
        }
    }
}