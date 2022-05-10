package no.nav.helse.økonomi

import java.time.LocalDate
import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import kotlin.math.roundToInt
import kotlin.properties.Delegates

internal class Økonomi private constructor(
    private val grad: Prosentdel,
    private var totalGrad: Prosentdel = grad,
    private var arbeidsgiverRefusjonsbeløp: Inntekt = INGEN,
    private var arbeidsgiverperiode: Arbeidsgiverperiode? = null,
    private val aktuellDagsinntekt: Inntekt = INGEN,
    private val dekningsgrunnlag: Inntekt = INGEN,
    private val skjæringstidspunkt: LocalDate? = null,
    private var grunnbeløpgrense: Inntekt? = null,
    private var arbeidsgiverbeløp: Inntekt? = null,
    private var personbeløp: Inntekt? = null,
    private var er6GBegrenset: Boolean? = null,
    private var tilstand: Tilstand = Tilstand.KunGrad,
) {

    companion object {
        private val arbeidsgiverBeløp = { økonomi: Økonomi -> økonomi.arbeidsgiverbeløp }
        private val personBeløp = { økonomi: Økonomi -> økonomi.personbeløp }

        internal fun sykdomsgrad(grad: Prosentdel) =
            Økonomi(grad)

        internal fun ikkeBetalt() = sykdomsgrad(0.prosent)

        internal fun ikkeBetalt(arbeidsgiverperiode: Arbeidsgiverperiode?) = ikkeBetalt().also {
            it.arbeidsgiverperiode = arbeidsgiverperiode
        }

        internal fun totalSykdomsgrad(økonomiList: List<Økonomi>) =
            Inntekt.vektlagtGjennomsnitt(økonomiList.map { it.grad() to it.dekningsgrunnlag })

        internal fun List<Økonomi>.avgrensTilArbeidsgiverperiode(periode: Periode): Periode? {
            return map { it.arbeidsgiverperiode }.firstOrNull()?.firstOrNull()?.let { førsteArbeidsgiverperiodedag ->
                Periode(førsteArbeidsgiverperiodedag, periode.endInclusive)
            }?.takeUnless { it == periode }
        }

        internal fun betal(økonomiList: List<Økonomi>, virkningsdato: LocalDate): List<Økonomi> = økonomiList.also {
            val totalSykdomsgrad = totalSykdomsgrad(økonomiList)
            økonomiList.forEach { økonomi -> økonomi.totalGrad = totalSykdomsgrad }
            delteUtbetalinger(it)
            fordelBeløp(it, virkningsdato)
        }

        private fun delteUtbetalinger(økonomiList: List<Økonomi>) = økonomiList.forEach { it.betal() }

        private fun fordelBeløp(økonomiList: List<Økonomi>, virkningsdato: LocalDate) {
            val totalArbeidsgiver = totalArbeidsgiver(økonomiList)
            val totalPerson = totalPerson(økonomiList)
            val total = totalArbeidsgiver + totalPerson
            if (total == INGEN) return økonomiList.forEach { økonomi -> økonomi.er6GBegrenset = false }

            val grunnlagForSykepengegrunnlag = økonomiList.map { økonomi -> økonomi.aktuellDagsinntekt }.summer() // TODO: må få denne fra Vilkårsgrunnlag
            val grunnbeløp = Grunnbeløp.`6G`.beløp(økonomiList.firstNotNullOf { økonomi -> økonomi.skjæringstidspunkt }, virkningsdato)
            økonomiList.forEach { it.grunnbeløpgrense = grunnbeløp }
            val sykepengegrunnlag = grunnlagForSykepengegrunnlag.avgrens(grunnbeløp).rundTilDaglig()
            val redusert = (sykepengegrunnlag * økonomiList.first().totalGrad).rundTilDaglig()
            val totalRefusjon = økonomiList.mapNotNull { økonomi -> økonomi.arbeidsgiverbeløp }.summer()

            check(totalRefusjon == totalArbeidsgiver)

            check(økonomiList.any { it.skjæringstidspunkt != null }) { "ingen økonomiobjekt har skjæringstidspunkt" }
            check(økonomiList.filter { it.skjæringstidspunkt != null }.distinctBy { it.skjæringstidspunkt }.count() == 1) { "det finnes flere unike skjæringstidspunkt for økonomiobjekt på samme dag" }

            val skjæringstidspunkt = økonomiList.firstNotNullOf { it.skjæringstidspunkt }
            økonomiList.forEach { it.grunnbeløpgrense = Grunnbeløp.`6G`.beløp(skjæringstidspunkt, virkningsdato) }

            fordelArbeidsgiverbeløp(økonomiList, totalRefusjon, redusert)
            val forventetPersonbeløp = økonomiList.map { it.dekningsgrunnlag * it.grad() }.summer() - totalArbeidsgiver
            check(forventetPersonbeløp == totalPerson) {
                "oh no: $forventetPersonbeløp != $totalPerson"
            }
            fordelPersonbeløp(økonomiList, forventetPersonbeløp, (redusert - totalArbeidsgiver(økonomiList)).rundTilDaglig())
            økonomiList.forEach { økonomi -> økonomi.er6GBegrenset = grunnlagForSykepengegrunnlag > grunnbeløp }
        }

        private fun fordelPersonbeløp(økonomiList: List<Økonomi>, total: Inntekt, grense: Inntekt) {
            val kvotient = reduksjon(grense, total)
            økonomiList.map { økonomi -> økonomi.personbeløp = økonomi.personbeløp?.times(kvotient) }
            val totaltRestbeløp = grense.reflection { _, _, _, dagligInt -> dagligInt }
            fordel(totaltRestbeløp, økonomiList, { økonomi, inntekt -> økonomi.personbeløp = inntekt }, personBeløp)
        }

        private fun fordelArbeidsgiverbeløp(økonomiList: List<Økonomi>, total: Inntekt, grense: Inntekt) {
            val kvotient = reduksjon(grense, total)
            // begrens utbetaling ihht forholdet mellom total refusjon og sykepengegrunnlag (justert for total sykdomsgrad og 6G)
            økonomiList.map { økonomi -> økonomi.arbeidsgiverbeløp = økonomi.arbeidsgiverbeløp?.times(kvotient) }
            val totaltRestbeløp = (total * kvotient - økonomiList.mapNotNull { it.arbeidsgiverbeløp?.rundNedTilDaglig() }.summer()).reflection { _, _, _, dagligInt -> dagligInt }
            fordel(totaltRestbeløp, økonomiList, { økonomi, inntekt -> økonomi.arbeidsgiverbeløp = inntekt }, arbeidsgiverBeløp)
        }

        private fun reduksjon(grense: Inntekt, total: Inntekt): Prosentdel {
            if (total == INGEN) return Prosentdel.NULL_PROSENT
            return Prosentdel.fraRatio((grense ratio total).coerceAtMost(1.0))
        }

        private fun fordel(rest: Int, økonomiList: List<Økonomi>, setter: (Økonomi, Inntekt?) -> Unit, beløpstrategi: (Økonomi) -> Inntekt?) {
            økonomiList
                .sortedByDescending { økonomi -> (beløpstrategi(økonomi) ?: INGEN).let { it - it.rundNedTilDaglig() }.reflection { _, _, daglig, _ -> daglig } }
                .onEach { økonomi -> setter(økonomi, beløpstrategi(økonomi)?.rundNedTilDaglig()) }
                .take(rest)
                .forEach { økonomi -> setter(økonomi, beløpstrategi(økonomi)?.plus(1.daglig)) }
        }

        private fun beregnUtbetalingFørAvrunding(
            økonomiList: List<Økonomi>,
            total: Inntekt,
            grense: Inntekt,
            get: (Økonomi) -> Inntekt,
        ): List<Beregningsresultat> {
            if (total <= 0.daglig) {
                return økonomiList.map { Beregningsresultat(økonomi = it, utbetalingFørAvrunding = 0.daglig) }
            }

            val ratio = grense ratio total

            return økonomiList
                .map {
                    val utbetalingFørAvrunding = get(it) * ratio.coerceAtMost(1.0)
                    Beregningsresultat(
                        økonomi = it,
                        utbetalingFørAvrunding = utbetalingFørAvrunding
                    )
                }
        }

        data class Beregningsresultat(
            internal val økonomi: Økonomi,
            internal val utbetalingFørAvrunding: Inntekt,
        ) {
            internal fun differanse() = (utbetalingFørAvrunding - utbetalingEtterAvrunding()).reflection { _, _, daglig, _ -> daglig }
            internal fun utbetalingEtterAvrunding() = utbetalingFørAvrunding.rundNedTilDaglig()
        }

        private fun totalArbeidsgiver(økonomiList: List<Økonomi>): Inntekt =
            økonomiList
                .map { it.arbeidsgiverbeløp ?: throw IllegalStateException("utbetalinger ennå ikke beregnet") }
                .summer()

        private fun totalPerson(økonomiList: List<Økonomi>): Inntekt =
            økonomiList
                .map { it.personbeløp ?: throw IllegalStateException("utbetalinger ennå ikke beregnet") }
                .summer()

        internal fun er6GBegrenset(økonomiList: List<Økonomi>) =
            økonomiList.any { it.er6GBegrenset() }
    }

    init {
        require(dekningsgrunnlag >= INGEN) { "dekningsgrunnlag kan ikke være negativ." }
    }

    internal fun inntekt(aktuellDagsinntekt: Inntekt, dekningsgrunnlag: Inntekt = aktuellDagsinntekt, skjæringstidspunkt: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode? = null): Økonomi =
        tilstand.inntekt(this, aktuellDagsinntekt, dekningsgrunnlag, skjæringstidspunkt, arbeidsgiverperiode)

    internal fun arbeidsgiverRefusjon(refusjonsbeløp: Inntekt?) =
        tilstand.arbeidsgiverRefusjon(this, refusjonsbeløp)

    internal fun lås() = tilstand.lås(this)

    internal fun låsOpp() = tilstand.låsOpp(this)

    internal fun builder(builder: ØkonomiBuilder) {
        tilstand.builder(this, builder)
    }

    private fun _buildKunGrad(builder: ØkonomiBuilder) {
        medData { grad, _, _, _, _, _, _, _, _ ->
            /* ikke legg på flere felter - alle er enten null eller har defaultverdi */
            builder.grad(grad)
        }
    }

    private fun _build(builder: ØkonomiBuilder) {
        medData { grad,
                  arbeidsgiverRefusjonsbeløp,
                  dekningsgrunnlag,
                  skjæringstidspunkt,
                  totalGrad,
                  aktuellDagsinntekt,
                  arbeidsgiverbeløp,
                  personbeløp,
                  er6GBegrenset ->
            builder.grad(grad)
                .arbeidsgiverRefusjonsbeløp(arbeidsgiverRefusjonsbeløp)
                .dekningsgrunnlag(dekningsgrunnlag)
                .skjæringstidspunkt(skjæringstidspunkt)
                .totalGrad(totalGrad)
                .aktuellDagsinntekt(aktuellDagsinntekt)
                .arbeidsgiverbeløp(arbeidsgiverbeløp)
                .personbeløp(personbeløp)
                .er6GBegrenset(er6GBegrenset)
                .arbeidsgiverperiode(arbeidsgiverperiode)
                .grunnbeløpsgrense(grunnbeløpgrense?.reflection { årlig, _, _, _ -> årlig })
                .tilstand(tilstand)
        }
    }

    internal fun <R> medData(lambda: MedØkonomiData<R>) = tilstand.medData(this, lambda)

    internal fun <R> medAvrundetData(
        block: (
            grad: Int,
            arbeidsgiverRefusjonsbeløp: Int,
            dekningsgrunnlag: Int,
            skjæringstidspunkt: LocalDate?,
            totalGrad: Int,
            aktuellDagsinntekt: Int,
            arbeidsgiverbeløp: Int?,
            personbeløp: Int?,
            er6GBegrenset: Boolean?
        ) -> R
    ) =
        medData { grad: Double,
                  arbeidsgiverRefusjonsbeløp: Double,
                  dekningsgrunnlag: Double,
                  skjæringstidspunkt: LocalDate?,
                  totalGrad: Double,
                  aktuellDagsinntekt: Double,
                  arbeidsgiverbeløp: Double?,
                  personbeløp: Double?,
                  er6GBegrenset: Boolean? ->
            block(
                grad.roundToInt(),
                arbeidsgiverRefusjonsbeløp.roundToInt(),
                dekningsgrunnlag.roundToInt(),
                skjæringstidspunkt,
                totalGrad.roundToInt(),
                aktuellDagsinntekt.roundToInt(),
                arbeidsgiverbeløp?.roundToInt(),
                personbeløp?.roundToInt(),
                er6GBegrenset
            )
        }

    internal fun <R> medData(block: (grad: Double, totalGrad: Double, aktuellDagsinntekt: Double) -> R) =
        medData { grad: Double,
                  _: Double,
                  _: Double?,
                  _: LocalDate?,
                  totalGrad: Double,
                  aktuellDagsinntekt: Double,
                  _: Double?,
                  _: Double?,
                  _: Boolean? ->
            block(grad, totalGrad, aktuellDagsinntekt)
        }

    internal fun medAvrundetData(block: (grad: Int, aktuellDagsinntekt: Int) -> Unit) {
        medAvrundetData { grad, _, _, _, _, aktuellDagsinntekt, _, _, _ -> block(grad, aktuellDagsinntekt) }
    }

    private fun <R> medDataFraBeløp(lambda: MedØkonomiData<R>) = lambda(
        grad.toDouble(),
        arbeidsgiverRefusjonsbeløp.reflection { _, _, daglig, _ -> daglig },
        dekningsgrunnlag.reflection { _, _, daglig, _ -> daglig },
        skjæringstidspunkt,
        totalGrad.toDouble(),
        aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig },
        arbeidsgiverbeløp!!.reflection { _, _, daglig, _ -> daglig },
        personbeløp!!.reflection { _, _, daglig, _ -> daglig },
        er6GBegrenset
    )

    private fun <R> medDataFraInntekt(lambda: MedØkonomiData<R>) = lambda(
        grad.toDouble(),
        arbeidsgiverRefusjonsbeløp.reflection { _, _, daglig, _ -> daglig },
        dekningsgrunnlag.reflection { _, _, daglig, _ -> daglig },
        skjæringstidspunkt,
        totalGrad.toDouble(),
        aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig },
        null, null, null
    )

    private fun grad() = tilstand.grad(this)

    private fun betal() = this.also { tilstand.betal(this) }

    internal fun er6GBegrenset() = tilstand.er6GBegrenset(this)

    internal fun harPersonbeløp() = personbeløp!! > INGEN

    private fun _betal() {
        val total = dekningsgrunnlag * grad().ratio()
        val gradertArbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp * grad().ratio()
        arbeidsgiverbeløp = gradertArbeidsgiverRefusjonsbeløp.coerceAtMost(total)
        personbeløp = (total - arbeidsgiverbeløp!!).coerceAtLeast(INGEN)
    }

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: NavDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: AvvistDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: NavHelgDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: ArbeidsgiverperiodeDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: Arbeidsdag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.Sykedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal sealed class Tilstand {

        internal open fun grad(økonomi: Økonomi) = økonomi.grad

        internal abstract fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>): R

        internal open fun builder(økonomi: Økonomi, builder: ØkonomiBuilder) {
            økonomi._build(builder)
        }

        internal open fun inntekt(
            økonomi: Økonomi,
            aktuellDagsinntekt: Inntekt,
            dekningsgrunnlag: Inntekt,
            skjæringstidspunkt: LocalDate,
            arbeidsgiverperiode: Arbeidsgiverperiode?
        ): Økonomi {
            throw IllegalStateException("Kan ikke sette inntekt i tilstand ${this::class.simpleName}")
        }

        internal open fun betal(økonomi: Økonomi) {
            throw IllegalStateException("Kan ikke beregne utbetaling i tilstand ${this::class.simpleName}")
        }

        internal open fun er6GBegrenset(økonomi: Økonomi): Boolean {
            throw IllegalStateException("Beløp er ikke beregnet ennå")
        }

        internal open fun lås(økonomi: Økonomi): Økonomi {
            throw IllegalStateException("Kan ikke låse Økonomi i tilstand ${this::class.simpleName}")
        }

        internal open fun låsOpp(økonomi: Økonomi): Økonomi {
            throw IllegalStateException("Kan ikke låse opp Økonomi i tilstand ${this::class.simpleName}")
        }

        internal open fun arbeidsgiverRefusjon(økonomi: Økonomi, refusjonsbeløp: Inntekt?): Økonomi {
            throw IllegalStateException("Kan ikke sette arbeidsgiverrefusjonsbeløp i tilstand ${this::class.simpleName}")
        }

        internal object KunGrad : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi

            override fun builder(økonomi: Økonomi, builder: ØkonomiBuilder) {
                økonomi._buildKunGrad(builder)
            }

            override fun arbeidsgiverRefusjon(økonomi: Økonomi, refusjonsbeløp: Inntekt?) = økonomi

            override fun inntekt(
                økonomi: Økonomi,
                aktuellDagsinntekt: Inntekt,
                dekningsgrunnlag: Inntekt,
                skjæringstidspunkt: LocalDate,
                arbeidsgiverperiode: Arbeidsgiverperiode?
            ) = Økonomi(
                grad = økonomi.grad,
                totalGrad = økonomi.totalGrad,
                arbeidsgiverperiode = arbeidsgiverperiode ?: økonomi.arbeidsgiverperiode,
                arbeidsgiverRefusjonsbeløp = økonomi.arbeidsgiverRefusjonsbeløp,
                aktuellDagsinntekt = aktuellDagsinntekt,
                dekningsgrunnlag = dekningsgrunnlag,
                skjæringstidspunkt = skjæringstidspunkt,
                grunnbeløpgrense = Grunnbeløp.`6G`.beløp(skjæringstidspunkt),
                tilstand = HarInntekt
            )

            override fun betal(økonomi: Økonomi) {
                økonomi.arbeidsgiverbeløp = INGEN
                økonomi.personbeløp = INGEN
                økonomi.tilstand = HarBeløp
            }

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) =
                lambda(
                    grad = økonomi.grad.toDouble(),
                    arbeidsgiverRefusjonsbeløp = økonomi.arbeidsgiverRefusjonsbeløp.reflection { _, _, daglig, _ -> daglig },
                    dekningsgrunnlag = økonomi.dekningsgrunnlag.reflection { _, _, daglig, _ -> daglig },
                    skjæringstidspunkt = null,
                    totalGrad = økonomi.totalGrad.toDouble(),
                    aktuellDagsinntekt = økonomi.aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig },
                    arbeidsgiverbeløp = null,
                    personbeløp = null,
                    er6GBegrenset = null
                )
        }

        internal object HarInntekt : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi.also {
                it.tilstand = Låst
            }

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) = økonomi.medDataFraInntekt(lambda)

            override fun arbeidsgiverRefusjon(økonomi: Økonomi, refusjonsbeløp: Inntekt?) = økonomi.apply {
                økonomi.arbeidsgiverRefusjonsbeløp = refusjonsbeløp ?: økonomi.aktuellDagsinntekt
            }

            override fun betal(økonomi: Økonomi) {
                økonomi._betal()
                økonomi.tilstand = HarBeløp
            }
        }

        internal object HarBeløp : Tilstand() {

            override fun er6GBegrenset(økonomi: Økonomi) = økonomi.er6GBegrenset!!

            override fun betal(økonomi: Økonomi) {
                økonomi._betal()
            }

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) = økonomi.medDataFraBeløp(lambda)
        }

        internal object Låst : Tilstand() {

            override fun grad(økonomi: Økonomi) = 0.prosent

            override fun lås(økonomi: Økonomi) = økonomi

            override fun låsOpp(økonomi: Økonomi) = økonomi.apply {
                tilstand = HarInntekt
            }

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) = økonomi.medDataFraInntekt(lambda)

            override fun betal(økonomi: Økonomi) {
                økonomi.arbeidsgiverbeløp = 0.daglig
                økonomi.personbeløp = 0.daglig
                økonomi.tilstand = LåstMedBeløp
            }
        }

        internal object LåstMedBeløp : Tilstand() {

            override fun grad(økonomi: Økonomi) = 0.prosent

            override fun lås(økonomi: Økonomi) = økonomi

            override fun låsOpp(økonomi: Økonomi) = økonomi.apply {
                tilstand = HarInntekt
            }

            override fun betal(økonomi: Økonomi) {}

            override fun er6GBegrenset(økonomi: Økonomi) = false

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) = økonomi.medDataFraBeløp(lambda)
        }
    }

    internal class Builder : ØkonomiBuilder() {
        fun build() = when (tilstand) {
            is Tilstand.KunGrad -> Økonomi(grad.prosent)
            else -> Økonomi(
                grad.prosent,
                totalGrad?.prosent!!,
                arbeidsgiverRefusjonsbeløp?.daglig!!,
                arbeidsgiverperiode,
                aktuellDagsinntekt?.daglig!!,
                dekningsgrunnlag?.daglig!!,
                skjæringstidspunkt,
                grunnbeløpgrense?.årlig,
                arbeidsgiverbeløp?.daglig,
                personbeløp?.daglig,
                er6GBegrenset,
                tilstand!!
            )
        }
    }
}

internal abstract class ØkonomiBuilder {
    protected var grad by Delegates.notNull<Double>()
    protected var arbeidsgiverRefusjonsbeløp: Double? = null
    protected var dekningsgrunnlag: Double? = null
    protected var skjæringstidspunkt: LocalDate? = null
    protected var totalGrad: Double? = null
    protected var aktuellDagsinntekt: Double? = null
    protected var arbeidsgiverbeløp: Double? = null
    protected var personbeløp: Double? = null
    protected var er6GBegrenset: Boolean? = null
    protected var arbeidsgiverperiode: Arbeidsgiverperiode? = null
    protected var grunnbeløpgrense: Double? = null
    protected var tilstand: Økonomi.Tilstand? = null


    internal fun grad(grad: Double): ØkonomiBuilder = apply {
        this.grad = grad
    }

    internal fun tilstand(tilstand: Økonomi.Tilstand): ØkonomiBuilder = apply {
        this.tilstand = tilstand
    }

    internal fun arbeidsgiverperiode(arbeidsgiverperiode: Arbeidsgiverperiode?) = apply {
        this.arbeidsgiverperiode = arbeidsgiverperiode
    }

    internal fun grunnbeløpsgrense(grunnbeløpgrense: Double?) = apply {
        this.grunnbeløpgrense = grunnbeløpgrense
    }

    internal fun arbeidsgiverRefusjonsbeløp(arbeidsgiverRefusjonsbeløp: Double?) = apply {
        this.arbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp
    }

    internal fun dekningsgrunnlag(dekningsgrunnlag: Double?) = apply {
        this.dekningsgrunnlag = dekningsgrunnlag
    }

    internal fun skjæringstidspunkt(skjæringstidspunkt: LocalDate?) = apply {
        this.skjæringstidspunkt = skjæringstidspunkt
    }

    internal fun totalGrad(totalGrad: Double?) = apply {
        this.totalGrad = totalGrad
    }

    internal fun aktuellDagsinntekt(aktuellDagsinntekt: Double?) = apply {
        this.aktuellDagsinntekt = aktuellDagsinntekt
    }

    internal fun arbeidsgiverbeløp(arbeidsgiverbeløp: Double?) = apply {
        this.arbeidsgiverbeløp = arbeidsgiverbeløp
    }

    internal fun personbeløp(personbeløp: Double?) = apply {
        this.personbeløp = personbeløp
    }

    internal fun er6GBegrenset(er6GBegrenset: Boolean?) = apply {
        this.er6GBegrenset = er6GBegrenset
    }
}

internal fun List<Økonomi>.totalSykdomsgrad(): Prosentdel = Økonomi.totalSykdomsgrad(this)

internal fun List<Økonomi>.betal(virkningsdato: LocalDate) = Økonomi.betal(this, virkningsdato)

internal fun List<Økonomi>.er6GBegrenset() = Økonomi.er6GBegrenset(this)

internal fun interface MedØkonomiData<R> {
    operator fun invoke(
        grad: Double,
        arbeidsgiverRefusjonsbeløp: Double,
        dekningsgrunnlag: Double,
        skjæringstidspunkt: LocalDate?,
        totalGrad: Double,
        aktuellDagsinntekt: Double,
        arbeidsgiverbeløp: Double?,
        personbeløp: Double?,
        er6GBegrenset: Boolean?
    ): R
}
