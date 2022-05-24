package no.nav.helse.økonomi

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.PeriodeData
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*
import no.nav.helse.økonomi.Inntekt.Companion.årlig

internal class CreateØkonomiTest {

    @Test
    fun `betale uten inntekt gir 0 i beløp`() {
        val data = sykdomstidslinjedag(79.5)
        createØkonomi(data).also { økonomi ->
            listOf(økonomi).betal(1.januar)
            økonomi.medData { grad, arbeidsgiverRefusjonsbeløp, dekningsgrunnlag, _, _, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, begrenset, _ ->
                assertEquals(79.5, grad)
                assertEquals(0.0, arbeidsgiverRefusjonsbeløp)
                assertEquals(0.0, dekningsgrunnlag)
                assertEquals(0.0, aktuellDagsinntekt)
                assertEquals(0.0, arbeidsgiverbeløp)
                assertEquals(0.0, personbeløp)
                no.nav.helse.testhelpers.assertNotNull(begrenset)
                assertFalse(begrenset)
            }
        }
    }

    @Test
    fun `opprette bare prosenter`() {
        val data = sykdomstidslinjedag(79.5)
        createØkonomi(data).also { økonomi ->
            økonomi.medData { grad, arbeidsgiverRefusjonsbeløp, dekningsgrunnlag, _, _, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, begrenset, grunnbeløpgrense ->
                assertEquals(79.5, grad)
                assertEquals(0.0, arbeidsgiverRefusjonsbeløp)
                assertEquals(0.0, dekningsgrunnlag)
                assertEquals(0.0, aktuellDagsinntekt)
                assertNull(arbeidsgiverbeløp)
                assertNull(personbeløp)
                assertNull(begrenset)
                assertNull(grunnbeløpgrense)
            }
            økonomi
                .inntekt(1200.daglig, skjæringstidspunkt = 1.januar)
                .medData { grad, arbeidsgiverRefusjonsbeløp, dekningsgrunnlag, skjæringstidspunkt, _, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, begrenset, grunnbeløpgrense ->
                    assertEquals(79.5, grad)
                    assertEquals(0.0, arbeidsgiverRefusjonsbeløp)
                    assertEquals(1200.0, dekningsgrunnlag)
                    assertEquals(1200.0, aktuellDagsinntekt)
                    assertEquals(1.januar, skjæringstidspunkt)
                    assertNull(arbeidsgiverbeløp)
                    assertNull(personbeløp)
                    assertNull(begrenset)
                    assertNull(grunnbeløpgrense)
                }
        }
    }

    @Test
    fun `kan sette arbeidsgiverperiode`() {
        val data = sykdomstidslinjedag(79.5)
        createØkonomi(data).also { økonomi ->
            assertDoesNotThrow { økonomi
                .inntekt(1200.daglig, skjæringstidspunkt = 1.januar, arbeidsgiverperiode = Arbeidsgiverperiode(listOf(1.januar til 16.januar)))
            }
        }
    }

    @Test
    fun `opprette med bare inntekt`() {
        val data = utbetalingsdag(80.0, 420.0, 1500.0, 1199.6)
        createØkonomi(data).also { økonomi ->
            økonomi.medData { grad,
                              arbeidsgiverRefusjonsbeløp,
                              dekningsgrunnlag,
                              _,
                              _,
                              aktuellDagsinntekt,
                              arbeidsgiverbeløp,
                              personbeløp,
                              er6GBegrenset,
                              grunnbeløpgrense ->
                assertEquals(80.0, grad)
                assertEquals(420.0, arbeidsgiverRefusjonsbeløp)
                assertEquals(1500.0, aktuellDagsinntekt)
                assertEquals(1199.6, dekningsgrunnlag)
                assertNull(arbeidsgiverbeløp)
                assertNull(personbeløp)
                assertNull(er6GBegrenset)
                assertNull(grunnbeløpgrense)
            }
            // Indirect test of Økonomi state is HarLønn
            assertThrows<IllegalStateException> { økonomi.inntekt(1200.daglig, skjæringstidspunkt = 1.januar) }
            assertDoesNotThrow { listOf(økonomi).betal(1.januar) }
        }
    }

    @Test
    fun `har betalinger`() {
        val data = utbetalingsdag(79.5, 420.0, 1500.0, 1199.6, 640.0, 320.0, 79.5, true)
        createØkonomi(data).also { økonomi ->
            økonomi.medData { grad,
                              arbeidsgiverRefusjon,
                              dekningsgrunnlag,
                              _,
                              totalGrad,
                              aktuellDagsinntekt,
                              arbeidsgiverbeløp,
                              personbeløp,
                              er6GBegrenset,
                              grunnbeløpgrense ->
                assertEquals(79.5, grad)
                assertEquals(79.5, totalGrad)
                assertEquals(420.0, arbeidsgiverRefusjon)
                assertEquals(1500.0, aktuellDagsinntekt)
                assertEquals(1199.6, dekningsgrunnlag)
                assertEquals(640.0, arbeidsgiverbeløp)
                assertEquals(320.0, personbeløp)
                assertTrue(er6GBegrenset as Boolean)
                assertEquals(Grunnbeløp.`6G`.beløp(1.januar), grunnbeløpgrense!!.årlig)
            }
            // Indirect test of Økonomi state
            assertThrows<IllegalStateException> { økonomi.inntekt(1200.daglig, skjæringstidspunkt = 1.januar) }
            assertDoesNotThrow { listOf(økonomi).betal(1.januar) }
        }
    }

    private fun utbetalingsdag(
        grad: Double,
        arbeidsgiverRefusjonsbeløp: Double,
        aktuellDagsinntekt: Double,
        dekningsgrunnlag: Double,
        arbeidsgiverbeløp: Double? = null,
        personbeløp: Double? = null,
        totalGrad: Double = grad,
        er6GBegrenset: Boolean = false,
        arbeidsgiverperiode: List<Periode>? = null
    ) = UtbetalingstidslinjeData.UtbetalingsdagData(
        UtbetalingstidslinjeData.TypeData.NavDag,
        arbeidsgiverperiode?.map { PeriodeData(it.start, it.endInclusive) },
        aktuellDagsinntekt,
        dekningsgrunnlag,
        1.januar,
        Grunnbeløp.`6G`.beløp(1.januar).reflection { årlig, _, _, _ -> årlig },
        null,
        null,
        grad,
        totalGrad,
        arbeidsgiverRefusjonsbeløp,
        arbeidsgiverbeløp,
        personbeløp,
        er6GBegrenset
    )

    private fun sykdomstidslinjedag(
        grad: Double,
    ) = PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        PersonData.ArbeidsgiverData.SykdomstidslinjeData.KildeData("type", UUID.randomUUID(), 1.januar.atStartOfDay()),
        grad,
        null
    )

    private fun createØkonomi(dagData: UtbetalingstidslinjeData.UtbetalingsdagData): Økonomi {
        lateinit var fangetØkonomi: Økonomi
        dagData.parseDag(1.januar).accept(object : UtbetalingsdagVisitor {
            override fun visit(
                dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
                dato: LocalDate,
                økonomi: Økonomi
            ) {
                fangetØkonomi = økonomi
            }
        })
        return fangetØkonomi
    }

    private fun createØkonomi(dagData: PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData): Økonomi {
        lateinit var fangetØkonomi: Økonomi
        dagData.parseDag(1.januar).accept(object : SykdomstidslinjeVisitor {
            override fun visitDag(
                dag: Dag.Sykedag,
                dato: LocalDate,
                økonomi: Økonomi,
                kilde: SykdomstidslinjeHendelse.Hendelseskilde
            ) {
                fangetØkonomi = økonomi
            }
        })
        return fangetØkonomi
    }
}
