package no.nav.helse.hendelser

import no.nav.helse.*
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.slutterEtter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class PeriodeTest {
    private val periode = Periode(1.juli, 10.juli)

    @Test
    fun `periode mellom`() {
        assertNull(periode.periodeMellom(periode.start.minusDays(1)))
        assertNull(periode.periodeMellom(periode.endInclusive))
        assertEquals(11.juli til 15.juli, periode.periodeMellom(16.juli))
    }

    @Test
    fun `overlapper med periode`() {
        assertTrue(periode.overlapperMed(Periode(20.juni, 10.juli)))
        assertTrue(periode.overlapperMed(Periode(20.juni, 1.juli)))
        assertTrue(periode.overlapperMed(Periode(2.juni, 7.juli)))
        assertTrue(periode.overlapperMed(Periode(1.juli, 12.juli)))
        assertTrue(periode.overlapperMed(Periode(10.juli, 12.juli)))
        assertTrue(periode.overlapperMed(Periode(30.juni, 11.juli)))
        assertTrue(periode.overlapperMed(periode))
    }

    @Test
    fun `inneholder med periode`() {
        assertTrue(periode.inneholder(periode))
        assertTrue(periode.inneholder(2.juli til 9.juli))
        assertFalse(periode.inneholder(periode.oppdaterFom(30.juni)))
        assertFalse(periode.inneholder(periode.oppdaterTom(11.juli)))
        assertFalse(periode.inneholder(29.juni til 30.juni))
        assertFalse(periode.inneholder(11.juli til 12.juli))
    }

    @Test
    fun `overlapper ikke med periode`() {
        assertFalse(periode.overlapperMed(Periode(1.april, 10.april)))
        assertFalse(periode.overlapperMed(Periode(1.april, 30.juni)))
        assertFalse(periode.overlapperMed(Periode(11.juli, 11.juli)))
        assertFalse(periode.overlapperMed(Periode(1.august, 10.august)))
    }

    @Test
    fun `fom kan være lik tom, men ikke før tom`() {
        assertDoesNotThrow { Periode(1.juli, 1.juli) }
        assertThrows<IllegalArgumentException> { Periode(2.juli, 1.juli) }
    }

    @Test
    fun `inneholder ikke ved delvis overlapp`() {
        assertFalse(periode.contains(Periode(10.juli, 11.juli)))
        assertFalse(periode.contains(Periode(30.juni, 1.juli)))
        assertFalse(Periode(10.juli, 11.juli) in periode)
        assertFalse(Periode(30.juni, 1.juli) in periode)
    }

    @Test
    fun `inneholder ikke perioder utenfor`() {
        assertFalse(periode.contains(Periode(11.juli, 12.juli)))
        assertFalse(periode.contains(Periode(29.juni, 30.juni)))
        assertFalse(Periode(11.juli, 12.juli) in periode)
        assertFalse(Periode(29.juni, 30.juni) in periode)
    }

    @Test
    fun `inneholder hele perioder`() {
        assertTrue(periode.contains(periode))
        assertTrue(periode.contains(Periode(10.juli, 10.juli)))
        assertTrue(periode.contains(Periode(1.juli, 1.juli)))
        assertTrue(periode in periode)
        assertTrue(Periode(10.juli, 10.juli) in periode)
        assertTrue(Periode(1.juli, 1.juli) in periode)
    }

    @Test
    fun `inneholder datoer`() {
        assertTrue(periode.contains(listOf(1.juli)))
        assertTrue(periode.contains(listOf(10.juli)))
        assertTrue(periode.contains(listOf(30.juni, 5.juli)))
        assertFalse(periode.contains(listOf(30.juni, 11.juli)))
        assertFalse(periode.contains(emptyList()))
    }

    @Test
    fun `inneholder enkeltdager`() {
        assertTrue(1.juli in periode)
        assertTrue(10.juli in periode)
        assertFalse(30.juni in periode)
        assertFalse(11.juli in periode)
    }

    @Test
    fun `perioder inneholder enkeltdager`() {
        val periode1 = Periode(1.juli, 2.juli)
        val periode2 = Periode(4.juli, 5.juli)
        val perioder = listOf(periode1, periode2)
        assertTrue(perioder.contains(1.juli))
        assertTrue(perioder.contains(5.juli))
        assertFalse(perioder.contains(3.juli))
        assertFalse(perioder.contains(6.juli))
        assertFalse(perioder.contains(30.juni))
    }

    @Test
    fun `slutter etter`() {
        assertTrue(periode.slutterEtter(periode.endInclusive))
        assertTrue(periode.slutterEtter(periode.endInclusive.minusDays(1)))
        assertFalse(periode.slutterEtter(periode.endInclusive.plusDays(1)))
    }

    @Test
    fun `periode før helg er rett før periode etter helg`() {
        val førHelg = Periode(4.januar, 5.januar)
        val helg = Periode(6.januar, 7.januar)
        val etterHelg = Periode(8.januar, 9.januar)
        val ukenEtter = Periode(9.januar, 12.januar)
        assertTrue(førHelg.erRettFør(helg))
        assertTrue(førHelg.erRettFør(etterHelg))
        assertFalse(førHelg.erRettFør(ukenEtter))
    }

    @Test
    fun `er utenfor en annen periode`() {
        val other = Periode(3.juli, 4.juli)
        assertFalse(Periode(3.juli, 4.juli).utenfor(other))
        assertTrue(Periode(2.juli, 4.juli).utenfor(other))
        assertTrue(Periode(1.juli, 2.juli).utenfor(other))
        assertTrue(Periode(3.juli, 5.juli).utenfor(other))
        assertTrue(Periode(5.juli, 6.juli).utenfor(other))
    }

    @Test
    fun `periode på eller etter dato`() {
        assertFalse(listOf(Periode(1.juli, 2.juli)).slutterEtter(3.juli))
        assertTrue(listOf(Periode(1.juli, 2.juli)).slutterEtter(2.juli))
        assertTrue(listOf(Periode(1.juli, 2.juli)).slutterEtter(1.juli))
        assertTrue(listOf(Periode(1.juli, 2.juli)).slutterEtter(30.juni))
    }

    @Test
    fun `oppdatere fom`() {
        val periode = Periode(2.januar, 3.januar)
        assertEquals(1.januar, periode.oppdaterFom(Periode(1.januar, 5.januar)).start)
        assertEquals(2.januar, periode.oppdaterFom(Periode(3.januar, 5.januar)).start)
    }

    @Test
    fun `oppdatere tom`() {
        val periode = Periode(2.januar, 3.januar)
        assertEquals(3.januar, periode.oppdaterTom(Periode(1.januar, 2.januar)).endInclusive)
        assertEquals(5.januar, periode.oppdaterTom(Periode(3.januar, 5.januar)).endInclusive)
    }

    @Test
    fun likhet() {
        val periode = Periode(2.januar, 3.januar)
        assertTrue(periode == periode)
        assertTrue(periode == Periode(2.januar, 3.januar))
        assertFalse(periode == Periode(periode.start, periode.endInclusive.plusDays(1)))
        assertFalse(periode == Periode(periode.start.plusDays(1), periode.endInclusive))
    }

    @Test
    fun `kan gjennomløpe tidslinjen`() {
        val actual = (1.januar til 5.januar).merge(15.januar til 19.januar)
        assertSize(19, actual)
    }

    @Test
    fun `slår sammen dager`() {
        assertTrue(emptyList<LocalDate>().grupperSammenhengendePerioder().isEmpty())
        assertEquals(listOf(1.januar til 1.januar), listOf(1.januar, 1.januar).grupperSammenhengendePerioder())
        assertEquals(listOf(1.januar til 1.januar), listOf(1.januar).grupperSammenhengendePerioder())
        assertEquals(listOf(1.januar til 2.januar), listOf(2.januar, 1.januar).grupperSammenhengendePerioder())
        assertEquals(listOf(1.januar til 1.januar, 3.januar til 3.januar), listOf(1.januar, 3.januar).grupperSammenhengendePerioder())
        assertEquals(
            listOf(1.januar til 5.januar, 8.januar til 8.januar),
            listOf(1.januar, 2.januar, 3.januar, 4.januar, 5.januar, 8.januar).grupperSammenhengendePerioder()
        )
    }

    @Test
    fun `strekk en periode for å dekke en annen periode`() {
        assertEquals(
            1.januar til 31.januar,
            (15.januar til 31.januar).merge(1.januar til 20.januar)
        )
    }

    @Test
    fun `tekstrepresentasjon av periode`() {
        assertEquals("01-01-2018 til 02-01-2018", Periode(1.januar, 2.januar).toString())
    }

    @Test
    fun subset() {
        val periode1 = 10.januar til 15.januar
        assertEquals(periode1, periode1.subset(1.januar til 31.januar))
        assertEquals(11.januar til 12.januar, periode1.subset(11.januar til 12.januar))
        assertEquals(2.januar til 2.januar, (1.januar til 2.januar).subset(2.januar til 12.januar))
        assertEquals(2.januar til 2.januar, (2.januar til 3.januar).subset(1.januar til 2.januar))
        assertThrows<IllegalArgumentException> { (1.januar til 2.januar).subset(11.januar til 12.januar) }
    }

    @Test
    fun beholdDagerEtter() {
        val periode = 1.februar til 28.februar
        assertEquals(null, periode.beholdDagerEtter(28.februar))
        assertEquals(2.februar til 28.februar, periode.beholdDagerEtter(1.februar))
        assertEquals(28.februar til 28.februar, periode.beholdDagerEtter(27.februar))
        assertEquals(16.februar til 28.februar, periode.beholdDagerEtter(15.februar))
        assertEquals(periode, periode.beholdDagerEtter(31.januar))
    }

    private fun assertSize(expected: Int, periode: Periode) {
        var count = 0
        periode.forEach { _ -> count++ }
        assertEquals(expected, count)

        count = 0
        for (date: LocalDate in periode) {
            count++
        }
        assertEquals(expected, count)
    }
}
