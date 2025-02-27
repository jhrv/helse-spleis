package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvis
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.periode
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi.Companion.totalSykdomsgrad

internal object Sykdomsgradfilter: UtbetalingstidslinjerFilter {

    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        perioder: List<Triple<Periode, IAktivitetslogg, SubsumsjonObserver>>
    ): List<Utbetalingstidslinje> {
        val tidslinjerForSubsumsjon = tidslinjer.subsumsjonsformat()
        val dagerUnderGrensen = periode(tidslinjer).filter { dato -> totalSykdomsgrad(tidslinjer.map { it[dato].økonomi }).erUnderGrensen() }

        avvis(tidslinjer, dagerUnderGrensen.grupperSammenhengendePerioder(), listOf(Begrunnelse.MinimumSykdomsgrad))

        perioder.forEach { (periode, aktivitetslogg, subsumsjonObserver) ->
            Prosentdel.subsumsjon(subsumsjonObserver) { grense ->
                `§ 8-13 ledd 2`(periode, tidslinjerForSubsumsjon, grense, dagerUnderGrensen)
            }
            val avvisteDager = avvisteDager(tidslinjer, periode, Begrunnelse.MinimumSykdomsgrad)
            val harAvvisteDager = avvisteDager.isNotEmpty()
            subsumsjonObserver.`§ 8-13 ledd 1`(periode, avvisteDager.map { it.dato }, tidslinjerForSubsumsjon)
            if (harAvvisteDager) aktivitetslogg.varsel("Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %. Vurder å sende vedtaksbrev fra Infotrygd")
            else aktivitetslogg.info("Ingen avviste dager på grunn av 20 %% samlet sykdomsgrad-regel for denne perioden")
        }
        return tidslinjer
    }
}
