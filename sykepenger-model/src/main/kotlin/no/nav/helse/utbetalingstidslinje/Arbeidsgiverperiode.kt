package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek
import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Periodetype
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.sykdomstidslinje.erRettFør

internal class Arbeidsgiverperiode private constructor(private val perioder: List<Periode>, førsteUtbetalingsdag: LocalDate?) : Iterable<LocalDate>, Comparable<LocalDate> {
    constructor(perioder: List<Periode>) : this(perioder, null)

    private val kjenteDager = mutableListOf<Periode>()
    private val utbetalingsdager = mutableListOf<Periode>()

    init {
        check(perioder.isNotEmpty() || førsteUtbetalingsdag != null) {
            "Enten må arbeidsgiverperioden være oppgitt eller så må første utbetalingsdag være oppgitt"
        }
        førsteUtbetalingsdag?.also { utbetalingsdag(it) }
    }

    private val arbeidsgiverperioden get() = perioder.first().start til perioder.last().endInclusive
    private val førsteKjente get() = listOfNotNull(perioder.firstOrNull()?.start, utbetalingsdager.firstOrNull()?.start, kjenteDager.firstOrNull()?.start).minOf { it }
    private val sisteKjente get() = listOfNotNull(perioder.lastOrNull()?.endInclusive, utbetalingsdager.lastOrNull()?.endInclusive, kjenteDager.lastOrNull()?.endInclusive).maxOf { it }
    private val innflytelseperioden get() = førsteKjente til sisteKjente

    internal fun fiktiv() = perioder.isEmpty()

    internal fun kjentDag(dagen: LocalDate) = apply {
        kjenteDager.add(dagen)
    }

    override fun compareTo(other: LocalDate) =
        førsteKjente.compareTo(other)

    operator fun contains(dato: LocalDate) =
        dato in innflytelseperioden

    operator fun contains(periode: Periode) =
        innflytelseperioden.overlapperMed(periode)

    internal fun ingenUtbetaling(periode: Periode, subsumsjonObserver: SubsumsjonObserver): Boolean {
        if (!dekker(periode)) return erFørsteUtbetalingsdagEtter(periode.endInclusive) || erSisteUtbetalingsdagFør(periode)
        subsumsjonObserver.`§ 8-17 ledd 1 bokstav a - arbeidsgiversøknad`(this)
        return true
    }

    internal fun dekker(periode: Periode): Boolean {
        if (fiktiv()) return false
        val heleInklHelg = arbeidsgiverperioden.justerForHelg()
        return (periode.overlapperMed(heleInklHelg) && heleInklHelg.slutterEtter(periode.endInclusive))
    }

    internal fun hørerTil(periode: Periode, sisteKjente: LocalDate = this.sisteKjente) =
        periode.overlapperMed(førsteKjente til sisteKjente)

    internal fun sammenlign(other: List<Periode>): Boolean {
        if (fiktiv()) return true
        val otherSiste = other.lastOrNull()?.endInclusive ?: return false
        val thisSiste = this.perioder.last().endInclusive
        return otherSiste == thisSiste || (thisSiste.erHelg() && otherSiste.erRettFør(thisSiste)) || (otherSiste.erHelg() && thisSiste.erRettFør(otherSiste))
    }

    internal fun erFørsteUtbetalingsdagEtter(dato: LocalDate) = utbetalingsdager.firstOrNull()?.start?.let { dato < it } ?: true

    // hvis det starter ny arbeidsgiverperioden i løpet av *periode* og siste utbetalingsdag var før perioden
    private fun erSisteUtbetalingsdagFør(periode: Periode): Boolean {
        return utbetalingsdager.last().endInclusive < periode.start
    }

    internal fun periodetype(organisasjonsnummer: String, other: Periode, skjæringstidspunkt: LocalDate, infotrygdhistorikk: Infotrygdhistorikk): Periodetype {
        val førsteUtbetalingsdag = utbetalingsdager.firstOrNull()?.start
        val avgjørende = førsteUtbetalingsdag?.let { maxOf(it, skjæringstidspunkt) } ?: skjæringstidspunkt
        return infotrygdhistorikk.periodetype(organisasjonsnummer, other, avgjørende) ?: if (avgjørende < other.start) return Periodetype.FORLENGELSE else Periodetype.FØRSTEGANGSBEHANDLING
    }

    override fun equals(other: Any?) = other is Arbeidsgiverperiode && other.førsteKjente == this.førsteKjente
    override fun hashCode() = førsteKjente.hashCode()

    override fun iterator(): Iterator<LocalDate> {
        return object : Iterator<LocalDate> {
            private val periodeIterators = perioder.map { it.iterator() }.iterator()
            private var current: Iterator<LocalDate>? = null

            override fun hasNext(): Boolean {
                val iterator = current
                if (iterator != null && iterator.hasNext()) return true
                if (!periodeIterators.hasNext()) return false
                current = periodeIterators.next()
                return true
            }

            override fun next(): LocalDate {
                return current?.next() ?: throw NoSuchElementException()
            }
        }
    }

    internal fun utbetalingsdag(dato: LocalDate) = apply {
        if (!dato.erHelg()) this.utbetalingsdager.add(dato)
        kjentDag(dato)
    }

    internal companion object {
        internal fun fiktiv(førsteUtbetalingsdag: LocalDate) = Arbeidsgiverperiode(emptyList(), førsteUtbetalingsdag)

        internal fun ingenUtbetaling(arbeidsgiverperiode: Arbeidsgiverperiode?, periode: Periode, subsumsjonObserver: SubsumsjonObserver) =
            arbeidsgiverperiode?.ingenUtbetaling(periode, subsumsjonObserver) ?: true

        internal fun List<Arbeidsgiverperiode>.finn(periode: Periode) = firstOrNull { arbeidsgiverperiode ->
            periode in arbeidsgiverperiode
        }

        private fun Periode.justerForHelg() = when (endInclusive.dayOfWeek) {
            DayOfWeek.SATURDAY -> start til endInclusive.plusDays(1)
            DayOfWeek.FRIDAY -> start til endInclusive.plusDays(2)
            else -> this
        }

        private fun MutableList<Periode>.add(dagen: LocalDate) {
            if (isNotEmpty() && last().endInclusive.plusDays(1) == dagen) {
                this[size - 1] = last().oppdaterTom(dagen)
            } else {
                add(dagen.somPeriode())
            }
        }
    }
}
