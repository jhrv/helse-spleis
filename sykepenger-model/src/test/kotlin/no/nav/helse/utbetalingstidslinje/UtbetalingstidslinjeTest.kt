package no.nav.helse.utbetalingstidslinje

import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.FOR
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.UkjentDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingstidslinjeTest {

    @Test
    fun `muterer ikke tidslinjen ved avvisning`() {
        val original = tidslinjeOf(5.NAV)
        val avvist1 = Utbetalingstidslinje.avvis(listOf(original), setOf(1.januar), listOf(Begrunnelse.MinimumSykdomsgrad)).first()
        val avvist2 = Utbetalingstidslinje.avvis(listOf(avvist1), setOf(1.januar), listOf(Begrunnelse.EtterDødsdato)).first()
        val avvist3 = Utbetalingstidslinje.avvis(listOf(avvist2), setOf(1.januar), listOf(Begrunnelse.ManglerMedlemskap)).first()
        assertFalse(original[1.januar] is AvvistDag)
        assertTrue(avvist1[1.januar] is AvvistDag)
        assertEquals(1, (avvist1[1.januar] as AvvistDag).begrunnelser.size)
        assertTrue(avvist2[1.januar] is AvvistDag)
        assertEquals(2, (avvist2[1.januar] as AvvistDag).begrunnelser.size)
        assertTrue(avvist3[1.januar] is AvvistDag)
        assertEquals(3, (avvist3[1.januar] as AvvistDag).begrunnelser.size)
    }

    @Test
    fun `avviser skjæringstidspunkt med flere begrunnelser`() {
        val original = tidslinjeOf(5.NAV)
        val avvist1 = Utbetalingstidslinje.avvis(listOf(original), setOf(1.januar), listOf(Begrunnelse.MinimumSykdomsgrad)).first()
        val avvist2 = Utbetalingstidslinje.avvis(listOf(avvist1), setOf(1.januar), listOf(Begrunnelse.EtterDødsdato)).first()
        val avvist3 = Utbetalingstidslinje.avvis(listOf(avvist2), setOf(1.januar), listOf(Begrunnelse.ManglerMedlemskap)).first()
        assertFalse(original[1.januar] is AvvistDag)
        assertTrue(avvist1[1.januar] is AvvistDag)
        assertEquals(listOf(Begrunnelse.MinimumSykdomsgrad), (avvist1[1.januar] as AvvistDag).begrunnelser)
        assertTrue(avvist2[1.januar] is AvvistDag)
        assertEquals(listOf(Begrunnelse.MinimumSykdomsgrad, Begrunnelse.EtterDødsdato), (avvist2[1.januar] as AvvistDag).begrunnelser)
        assertTrue(avvist3[1.januar] is AvvistDag)
        assertEquals(listOf(Begrunnelse.MinimumSykdomsgrad, Begrunnelse.EtterDødsdato, Begrunnelse.ManglerMedlemskap), (avvist3[1.januar] as AvvistDag).begrunnelser)
    }

    @Test
    fun `avviser perioder med flere begrunnelser`() {
        val periode = 1.januar til 5.januar
        val original = tidslinjeOf(5.NAV)
        val avvist1 = Utbetalingstidslinje.avvis(listOf(original), listOf(periode), listOf(Begrunnelse.MinimumSykdomsgrad)).first()
        val avvist2 = Utbetalingstidslinje.avvis(listOf(avvist1), listOf(periode), listOf(Begrunnelse.EtterDødsdato)).first()
        val avvist3 = Utbetalingstidslinje.avvis(listOf(avvist2), listOf(periode), listOf(Begrunnelse.ManglerMedlemskap)).first()
        periode.forEach { dato ->
            val dag = avvist3[dato] as AvvistDag
            assertEquals(3, dag.begrunnelser.size)
        }
    }

    @Test
    fun `samlet periode`() {
        assertEquals(1.januar til 1.januar, Utbetalingstidslinje.periode(listOf(tidslinjeOf(1.NAV))))
        assertEquals(1.desember(2017) til 7.mars, Utbetalingstidslinje.periode(listOf(
            tidslinjeOf(7.NAV),
            tidslinjeOf(7.NAV, startDato = 1.mars),
            tidslinjeOf(7.NAV, startDato = 1.desember(2017)),
        )))
    }

    @Test
    fun `betalte dager`() {
        tidslinjeOf(4.AP, 1.FRI, 6.NAV, 1.AVV, 2.FRI, 5.ARB).also {
            assertTrue(it.harBetalt(1.januar))
            assertFalse(it.harBetalt(5.januar))
            assertTrue(it.harBetalt(6.januar))
            assertTrue(it.harBetalt(7.januar))
            assertTrue(it.harBetalt(12.januar))
            assertFalse(it.harBetalt(15.januar))
            assertTrue(it.harBetalt(1.januar til 7.januar))
            assertFalse(it.harBetalt(13.januar til 20.januar))
        }
    }


    @Test
    fun `fri i helg mappes til ukjentdag`() {
        val tidslinje = tidslinjeOf(4.NAV, 3.FRI, 7.NAV)
        Utbetalingstidslinje.konverter(tidslinje).also {
            assertTrue(it[1.januar] is Dag.Sykedag)
            assertTrue(it[5.januar] is Dag.Feriedag)
            assertTrue(it[6.januar] is Dag.UkjentDag)
            assertTrue(it[8.januar] is Dag.Sykedag)
            assertTrue(it[13.januar] is Dag.SykHelgedag)
        }
    }

    @Test
    fun `mapper utbetalingstidslinje til sykdomstidslinje`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV, 4.ARB)
        Utbetalingstidslinje.konverter(tidslinje).also {
            assertTrue(it[1.januar] is Dag.Sykedag)
            assertTrue(it[6.januar] is Dag.SykHelgedag)
            assertTrue(it[31.januar] is Dag.Sykedag)
            assertTrue(it[1.februar] is Dag.Arbeidsdag)
            assertTrue(it[2.februar] is Dag.Arbeidsdag)
            assertTrue(it[3.februar] is Dag.Arbeidsdag)
            assertTrue(it[4.februar] is Dag.Arbeidsdag)
        }
    }

    @Test
    fun `konverterer feriedager, avviste dager og foreldet dager`() {
        val tidslinje = tidslinjeOf(7.NAV, 5.FOR, 2.FRI, 5.AVV, 7.FRI)
        Utbetalingstidslinje.konverter(tidslinje).also {
            assertTrue(it[1.januar] is Dag.Sykedag)
            assertTrue(it[6.januar] is Dag.SykHelgedag)
            assertTrue(it[8.januar] is Dag.ForeldetSykedag)
            assertTrue(it[15.januar] is Dag.Sykedag)
            assertTrue(it[22.januar] is Dag.Feriedag)
        }
    }

    @Test
    fun `sammenhengende perioder brytes opp av arbeidsdager`() {
        val tidslinje = tidslinjeOf(5.NAV, 1.ARB, 5.NAV)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(2, result.size)
        assertEquals(1.januar til 5.januar, result.first().periode())
        assertEquals(7.januar til 11.januar, result.last().periode())
    }

    @Test
    fun `sammenhengende perioder brytes opp av ukjent dag`() {
        val tidslinje = medInfotrygdtidslinje(tidslinjeOf(11.NAV), tidslinjeOf(1.NAV, startDato = 6.januar))
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(2, result.size)
        assertEquals(1.januar til 5.januar, result.first().periode())
        assertEquals(7.januar til 11.januar, result.last().periode())
    }

    @Test
    fun `fjerner ledende fridager`() {
        val tidslinje = tidslinjeOf(6.FRI, 5.NAV)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(1, result.size)
        assertEquals(7.januar til 11.januar, result.first().periode())
    }

    @Test
    fun `helg blir ikke sett på som en periode`() {
        val tidslinje = tidslinjeOf(5.ARB, 2.NAV, 5.ARB)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `foreldet perioder tas med`() {
        val tidslinje = tidslinjeOf(5.NAV, 5.FOR)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(1, result.size)
        assertEquals(1.januar til 10.januar, result.first().periode())
    }

    private fun medInfotrygdtidslinje(tidslinje: Utbetalingstidslinje, other: Utbetalingstidslinje) =
        tidslinje.plus(other) { actual, challenger ->
            when (challenger) {
                is NavDag, is NavHelgDag -> UkjentDag(actual.dato, actual.økonomi)
                else -> actual
            }
        }
}
