package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeVisitor
import no.nav.helse.serde.api.dto.AvvistDag
import no.nav.helse.serde.api.dto.BegrunnelseDTO
import no.nav.helse.serde.api.dto.NavDag
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag
import no.nav.helse.serde.api.dto.SykdomstidslinjedagKildetype
import no.nav.helse.serde.api.dto.SykdomstidslinjedagType
import no.nav.helse.serde.api.dto.Utbetalingstidslinjedag
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagMedGrad
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagUtenGrad
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi

// Besøker hele sykdomshistorikk-treet
internal class VedtaksperiodeSykdomstidslinjeBuilder(vedtaksperiode: Vedtaksperiode): VedtaksperiodeVisitor {
    private val sykdomstidslinje: MutableList<Sykdomstidslinjedag> = mutableListOf()
    init {
        vedtaksperiode.accept(this)
    }

    internal fun build() = sykdomstidslinje.toList()

    override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: MutableList<Periode>) {
        this.sykdomstidslinje.addAll(SykdomstidslinjeBuilder(tidslinje).build())
    }
}

internal class SykdomshistorikkBuilder(private val id: UUID, element: Sykdomshistorikk.Element) : SykdomshistorikkVisitor {
    private val beregnetTidslinje: MutableList<Sykdomstidslinjedag> = mutableListOf()

    init {
        element.accept(this)
    }

    fun build(): Pair<UUID, List<Sykdomstidslinjedag>> = id to beregnetTidslinje.toList()

    override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
        this.beregnetTidslinje.addAll(SykdomstidslinjeBuilder(tidslinje).build())
    }

}

internal class SykdomstidslinjeBuilder(tidslinje: Sykdomstidslinje): SykdomstidslinjeVisitor {
    private val tidslinje = mutableListOf<Sykdomstidslinjedag>()
    init {
        tidslinje.accept(this)
    }

    fun build() = tidslinje.toList()

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: Dag.Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) = leggTilDag(dag, dato, økonomi, kilde)

    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) =
        leggTilDag(dag, dato, null, kilde)

    override fun visitDag(
        dag: Dag.ProblemDag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde,
        other: SykdomstidslinjeHendelse.Hendelseskilde?,
        melding: String
    ) =
        leggTilDag(dag, dato, null, kilde)

    private fun leggTilDag(dag: Dag, dato: LocalDate, økonomi: Økonomi?, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        val dagDto = Sykdomstidslinjedag(
            dato,
            dag.toDagtypeDTO(),
            Sykdomstidslinjedag.SykdomstidslinjedagKilde(kilde.toKildetypeDTO(), kilde.meldingsreferanseId()),
            økonomi?.medData { grad, _, _ -> grad }
        )

        tidslinje.add(dagDto)
    }

    private fun SykdomstidslinjeHendelse.Hendelseskilde.toKildetypeDTO() = when {
        erAvType(Inntektsmelding::class) -> SykdomstidslinjedagKildetype.Inntektsmelding
        erAvType(Søknad::class) -> SykdomstidslinjedagKildetype.Søknad
        erAvType(Sykmelding::class) -> SykdomstidslinjedagKildetype.Sykmelding
        erAvType(OverstyrTidslinje::class) -> SykdomstidslinjedagKildetype.Saksbehandler
        else -> SykdomstidslinjedagKildetype.Ukjent
    }

    private fun Dag.toDagtypeDTO() = when (this) {
        is Dag.Sykedag -> SykdomstidslinjedagType.SYKEDAG
        is Dag.UkjentDag -> SykdomstidslinjedagType.ARBEIDSDAG
        is Dag.Arbeidsdag -> SykdomstidslinjedagType.ARBEIDSDAG
        is Dag.Arbeidsgiverdag -> SykdomstidslinjedagType.ARBEIDSGIVERDAG
        is Dag.Feriedag -> SykdomstidslinjedagType.FERIEDAG
        is Dag.FriskHelgedag -> SykdomstidslinjedagType.FRISK_HELGEDAG
        is Dag.ArbeidsgiverHelgedag -> SykdomstidslinjedagType.ARBEIDSGIVERDAG
        is Dag.ForeldetSykedag -> SykdomstidslinjedagType.FORELDET_SYKEDAG
        is Dag.SykHelgedag -> SykdomstidslinjedagType.SYK_HELGEDAG
        is Dag.Permisjonsdag -> SykdomstidslinjedagType.PERMISJONSDAG
        is Dag.ProblemDag -> SykdomstidslinjedagType.UBESTEMTDAG
    }
}

// Besøker hele utbetaling-treet
internal class UtbetalingstidslinjeBuilder(utbetaling: Utbetaling): UtbetalingVisitor {
    private val utbetalingstidslinje: MutableList<Utbetalingstidslinjedag> = mutableListOf()

    init {
        utbetaling.accept(this)
    }

    internal fun build() = utbetalingstidslinje.toList()

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medAvrundetData { _, aktuellDagsinntekt ->
            utbetalingstidslinje.add(
                UtbetalingstidslinjedagUtenGrad(
                    type = UtbetalingstidslinjedagType.Arbeidsdag,
                    inntekt = aktuellDagsinntekt,
                    dato = dato
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medAvrundetData { _, aktuellDagsinntekt ->
            utbetalingstidslinje.add(
                UtbetalingstidslinjedagUtenGrad(
                    type = UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag,
                    inntekt = aktuellDagsinntekt,
                    dato = dato
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        // TODO: Trenger speil _egentlig_ doubles?
        val (grad, totalGrad) = økonomi.medData { grad, totalGrad, _ -> grad to totalGrad }
        økonomi.medAvrundetData { _, refusjonsbeløp, _, _, _, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, _ ->
            utbetalingstidslinje.add(
                NavDag(
                    type = UtbetalingstidslinjedagType.NavDag,
                    inntekt = aktuellDagsinntekt,
                    dato = dato,
                    utbetaling = arbeidsgiverbeløp!!,
                    arbeidsgiverbeløp = arbeidsgiverbeløp,
                    personbeløp = personbeløp!!,
                    refusjonsbeløp = refusjonsbeløp,
                    grad = grad,
                    totalGrad = totalGrad
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medData { grad, _, _ ->
            utbetalingstidslinje.add(
                UtbetalingstidslinjedagMedGrad(
                    type = UtbetalingstidslinjedagType.NavHelgDag,
                    inntekt = 0,   // Speil needs zero here
                    dato = dato,
                    grad = grad
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(
            UtbetalingstidslinjedagUtenGrad(
                type = if (dato.erHelg()) UtbetalingstidslinjedagType.Helgedag else UtbetalingstidslinjedagType.Feriedag,
                inntekt = 0,    // Speil needs zero here
                dato = dato
            )
        )
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(
            UtbetalingstidslinjedagUtenGrad(
                type = UtbetalingstidslinjedagType.UkjentDag,
                inntekt = 0,    // Speil needs zero here
                dato = dato
            )
        )
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medData { _, totalGrad, _ ->
            utbetalingstidslinje.add(
                AvvistDag(
                    type = UtbetalingstidslinjedagType.AvvistDag,
                    inntekt = 0, // Speil needs zero here
                    dato = dato,
                    begrunnelser = dag.begrunnelser.map { BegrunnelseDTO.fraBegrunnelse(it) },
                    grad = 0.0,  // Speil wants zero here
                    totalGrad = totalGrad
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinje.add(
            UtbetalingstidslinjedagUtenGrad(
                type = UtbetalingstidslinjedagType.ForeldetDag,
                inntekt = 0,    // Speil needs zero here
                dato = dato
            )
        )
    }
}
