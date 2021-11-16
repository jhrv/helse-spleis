package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import java.time.LocalDate

internal class ArbeidsgiverUtbetalinger(
    private val regler: ArbeidsgiverRegler,
    private val arbeidsgivere: Map<Arbeidsgiver, IUtbetalingstidslinjeBuilder>,
    private val infotrygdhistorikk: Infotrygdhistorikk,
    private val alder: Alder,
    private val dødsdato: LocalDate?,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
) {
    internal lateinit var sykepengerettighet: Sykepengerettighet

    internal fun beregn(aktivitetslogg: IAktivitetslogg, organisasjonsnummer: String, periode: Periode, virkningsdato: LocalDate = periode.endInclusive) {
        val tidslinjer = arbeidsgivere.mapValues { (arbeidsgiver, builder) ->
            arbeidsgiver.build(builder, periode)
        }.filterValues { it.isNotEmpty() }
        filtrer(aktivitetslogg, tidslinjer, periode, virkningsdato)
        tidslinjer.forEach { (arbeidsgiver, utbetalingstidslinje) ->
            arbeidsgiver.lagreUtbetalingstidslinjeberegning(organisasjonsnummer, utbetalingstidslinje)
        }
    }

    private fun filtrer(aktivitetslogg: IAktivitetslogg, arbeidsgivere: Map<Arbeidsgiver, Utbetalingstidslinje>, periode: Periode, virkningsdato: LocalDate) {
        val tidslinjer = arbeidsgivere.values.toList()
        Sykdomsgradfilter(tidslinjer, periode, aktivitetslogg).filter()
        AvvisDagerEtterDødsdatofilter(tidslinjer, periode, dødsdato, aktivitetslogg).filter()
        AvvisDagerEtterFylte70ÅrFilter(tidslinjer, periode, alder, aktivitetslogg).filter()
        vilkårsgrunnlagHistorikk.avvisUtbetalingsdagerMedBegrunnelse(tidslinjer, alder)
        sykepengerettighet = MaksimumSykepengedagerfilter(alder, regler, periode, aktivitetslogg).filter(
            tidslinjer,
            infotrygdhistorikk.utbetalingstidslinje().kutt(periode.endInclusive)
        )
        arbeidsgivere.forEach { (arbeidsgiver, tidslinje) ->
            Refusjonsgjødsler(
                tidslinje = tidslinje + arbeidsgiver.infotrygdUtbetalingstidslinje(),
                refusjonshistorikk = arbeidsgiver.refusjonshistorikk,
                infotrygdhistorikk = infotrygdhistorikk,
                organisasjonsnummer = arbeidsgiver.organisasjonsnummer()
            ).gjødsle(aktivitetslogg, periode)
        }
        MaksimumUtbetaling(tidslinjer, aktivitetslogg, virkningsdato).betal()
    }
}
