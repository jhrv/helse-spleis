package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.økonomi.Økonomi

internal class MaksimumSykepengedagerfilter(
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val infotrygdtidslinje: Utbetalingstidslinje
): UtbetalingstidslinjerFilter, UtbetalingsdagVisitor {

    private companion object {
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26 * 7
    }

    private lateinit var maksimumSykepenger: Alder.MaksimumSykepenger
    private lateinit var sisteDag: LocalDate
    private lateinit var state: State
    private lateinit var teller: UtbetalingTeller
    private var opphold = 0
    private val begrunnelserForAvvisteDager = mutableMapOf<Begrunnelse, MutableSet<LocalDate>>()
    private val avvisteDager get() = begrunnelserForAvvisteDager.values.flatten().toSet()
    private lateinit var beregnetTidslinje: Utbetalingstidslinje
    private lateinit var tidslinjegrunnlag: List<Utbetalingstidslinje>

    private val tidslinjegrunnlagsubsumsjon by lazy { tidslinjegrunnlag.subsumsjonsformat() }
    private val beregnetTidslinjesubsumsjon by lazy { beregnetTidslinje.subsumsjonsformat() }

    internal fun maksimumSykepenger() = maksimumSykepenger

    internal fun maksimumSykepenger(builder: Utbetaling.Builder) {
        builder.maksimumSykepenger(
            sisteDag = maksimumSykepenger.sisteDag(),
            gjenståendeSykedager = maksimumSykepenger.gjenståendeDager(),
            forbrukteSykedager = maksimumSykepenger.forbrukteDager()
        )
    }

    private fun avvisDag(dag: LocalDate, begrunnelse: Begrunnelse) {
        begrunnelserForAvvisteDager.getOrPut(begrunnelse) {
            mutableSetOf()
        }.add(dag)
    }

    private fun karantenesporing(periode: Periode, subsumsjonObserver: SubsumsjonObserver) = object : Alder.MaksimumSykepenger.Begrunnelse {
        override fun `§ 8-12 ledd 1 punktum 1`(sisteDag: LocalDate, forbrukteDager: Int, gjenståendeDager: Int) {
            val sakensStartdato = teller.startdatoSykepengerettighet() ?: return
            subsumsjonObserver.`§ 8-12 ledd 1 punktum 1`(
                periode,
                tidslinjegrunnlagsubsumsjon,
                beregnetTidslinjesubsumsjon,
                gjenståendeDager,
                forbrukteDager,
                sisteDag,
                sakensStartdato
            )
        }

        override fun `§ 8-51 ledd 3`(sisteDag: LocalDate, forbrukteDager: Int, gjenståendeDager: Int) {
            val sakensStartdato = teller.startdatoSykepengerettighet() ?: return
            subsumsjonObserver.`§ 8-51 ledd 3`(
                periode,
                tidslinjegrunnlagsubsumsjon,
                beregnetTidslinjesubsumsjon,
                gjenståendeDager,
                forbrukteDager,
                sisteDag,
                sakensStartdato
            )
        }

        override fun `§ 8-3 ledd 1 punktum 2`(sisteDag: LocalDate, forbrukteDager: Int, gjenståendeDager: Int) {}
    }

    private var perioder = emptyList<Triple<Periode, IAktivitetslogg, SubsumsjonObserver>>()
    private fun subsumsjonObserver(dagen: LocalDate): SubsumsjonObserver {
        return perioder.firstOrNull { dagen in it.first }?.third ?: NullObserver
    }

    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        perioder: List<Triple<Periode, IAktivitetslogg, SubsumsjonObserver>>
    ): List<Utbetalingstidslinje> {
        this.perioder = perioder
        beregnetTidslinje = tidslinjer
            .reduce(Utbetalingstidslinje::plus)
            .plus(infotrygdtidslinje)
        tidslinjegrunnlag = tidslinjer + listOf(infotrygdtidslinje)
        teller = UtbetalingTeller(alder, arbeidsgiverRegler)
        state = State.Initiell
        beregnetTidslinje.accept(this)

        maksimumSykepenger = teller.maksimumSykepenger(sisteDag).also {
            perioder.forEach { (periode, _, jurist) -> it.sisteDag(karantenesporing(periode, jurist)) }
        }

        begrunnelserForAvvisteDager.forEach { (begrunnelse, avvisteDager) ->
            Utbetalingstidslinje.avvis(
                tidslinjer = tidslinjer,
                avvistePerioder = avvisteDager.grupperSammenhengendePerioder(),
                begrunnelser = listOf(begrunnelse)
            )
        }
        perioder.forEach { (periode, aktivitetslogg, subsumsjonObserver) ->
            // TODO: logge juridisk vurdering for 70 år i "karantenesporing"
            alder.etterlevelse70år(aktivitetslogg, beregnetTidslinje.periode(), avvisteDager, subsumsjonObserver)

            if (begrunnelserForAvvisteDager[Begrunnelse.NyVilkårsprøvingNødvendig]?.any { it in periode } == true) {
                aktivitetslogg.funksjonellFeil("Bruker er fortsatt syk 26 uker etter maksdato")
            }
            if (avvisteDager in periode)
                aktivitetslogg.info("Maks antall sykepengedager er nådd i perioden")
            else
                aktivitetslogg.info("Maksimalt antall sykedager overskrides ikke i perioden")
        }

        return tidslinjer
    }

    private fun state(nyState: State) {
        if (this.state == nyState) return
        state.leaving(this)
        state = nyState
        state.entering(this)
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        sisteDag = tidslinje.periode().endInclusive
    }

    override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {

    }

    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        if (alder.mistetSykepengerett(dato)) state(State.ForGammel)
        state.betalbarDag(this, dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        if (alder.mistetSykepengerett(dato)) state(State.ForGammel)
        state.sykdomshelg(this, dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        state.fridag(this, dato)
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dato)
    }

    override fun visit(
        dag: UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        oppholdsdag(dato)
    }

    private fun oppholdsdag(dagen: LocalDate) {
        opphold += 1
        state.oppholdsdag(this, dagen)
    }

    private fun nextState(dagen: LocalDate): State? {
        val maksimumSykepenger = teller.maksimumSykepenger(dagen)
        (opphold >= TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).let { harTilstrekkeligOpphold ->
            subsumsjonObserver(dagen).`§ 8-12 ledd 2`(
                oppfylt = harTilstrekkeligOpphold,
                dato = dagen,
                gjenståendeSykepengedager = maksimumSykepenger.gjenståendeDager(),
                beregnetAntallOppholdsdager = opphold,
                tilstrekkeligOppholdISykedager = TILSTREKKELIG_OPPHOLD_I_SYKEDAGER,
                tidslinjegrunnlag = tidslinjegrunnlagsubsumsjon,
                beregnetTidslinje = beregnetTidslinjesubsumsjon,
            )
            if (harTilstrekkeligOpphold) {
                teller.resett()
                return State.Initiell
            }
        }
        if (state == State.Karantene) return null
        if (maksimumSykepenger.sisteDag() > dagen) return null
        return State.Karantene
    }

    private interface State {
        fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {}
        fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) = avgrenser.oppholdsdag(dagen)
        fun entering(avgrenser: MaksimumSykepengedagerfilter) {}
        fun leaving(avgrenser: MaksimumSykepengedagerfilter) {}

        object Initiell : State {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                avgrenser.opphold = 0
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.teller.inkrementer(dagen)
                avgrenser.teller.dekrementer(dagen)
                avgrenser.state(Syk)
            }
        }

        object Syk : State {
            override fun entering(avgrenser: MaksimumSykepengedagerfilter) {
                avgrenser.opphold = 0
            }

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.teller.inkrementer(dagen)
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
                avgrenser.opphold = 0
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.state(avgrenser.nextState(dagen) ?: Opphold)
            }

            override fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
            }
        }

        object Opphold : State {

            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.teller.inkrementer(dagen)
                avgrenser.teller.dekrementer(dagen)
                avgrenser.state(avgrenser.nextState(dagen) ?: Syk)
            }

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
                oppholdsdag(avgrenser, dagen)
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }
        }

        object Karantene : State {
            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
                avgrenser.avvisDag(dagen, when (avgrenser.opphold > TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) {
                    true -> Begrunnelse.NyVilkårsprøvingNødvendig
                    false -> avgrenser.teller.maksimumSykepenger().begrunnelse()
                })
            }

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
                /* helg skal ikke medføre ny rettighet */
            }

            override fun oppholdsdag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.nextState(dagen)?.run { avgrenser.state(this) }
            }

            override fun fridag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.opphold += 1
            }
        }

        object ForGammel : State {
            override fun betalbarDag(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                over70(avgrenser, dagen)
            }

            override fun sykdomshelg(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                over70(avgrenser, dagen)
            }

            override fun leaving(avgrenser: MaksimumSykepengedagerfilter) = throw IllegalStateException("Kan ikke gå ut fra state ForGammel")

            private fun over70(avgrenser: MaksimumSykepengedagerfilter, dagen: LocalDate) {
                avgrenser.avvisDag(dagen, Begrunnelse.Over70)
            }
        }
    }
}
