package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.api.TilstandstypeDTO
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class KansellerUtbetalingTest : AbstractEndToEndTest() {

    @BeforeEach
    internal fun setup() {
        nyttVedtak(3.januar, 26.januar, 100, 3.januar)
    }

    @Test
    fun `avvis hvis arbeidsgiver er ukjent`() {
        håndterKansellerUtbetaling(orgnummer = "999999")
        inspektør.also {
            assertTrue(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
        }
    }

    @Test
    fun `avvis hvis vi ikke finner fagsystemId`() {
        håndterKansellerUtbetaling(fagsystemId = "unknown")
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, it.sisteTilstand(0))
        }
    }

    @Test
    fun `kanseller siste utbetaling`() {
        val behovTeller = inspektør.personLogg.behov().size
        håndterKansellerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        sjekkAt(inspektør) {
            !personLogg.hasErrorsOrWorse() ellers personLogg.toString()
            val behov = sisteBehov(Behovtype.Utbetaling)

            @Suppress("UNCHECKED_CAST")
            val statusForUtbetaling = (behov.detaljer()["linjer"] as List<Map<String, Any>>)[0]["statuskode"]
            statusForUtbetaling er "OPPH"
        }

        sjekkAt(speilApi().arbeidsgivere[0]) {
            vedtaksperioder[0].tilstand er TilstandstypeDTO.TilAnnullering
        }

        håndterUtbetalt(1.vedtaksperiode, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT, annullert = true)
        sjekkAt(inspektør) {
            !personLogg.hasErrorsOrWorse() ellers personLogg.toString()
            arbeidsgiverOppdrag.size er 2
            (personLogg.behov().size - behovTeller) skalVære 1 ellers personLogg.toString()
        }

        sjekkAt(TestOppdragInspektør(inspektør.arbeidsgiverOppdrag[1])) {
            linjer[0] er Utbetalingslinje(19.januar, 26.januar, 1431, 1431, 100.0)
            endringskoder[0] er Endringskode.ENDR
            refFagsystemIder[0] er null
        }

        sjekkAt(inspektør.personLogg.behov().last()) {
            type er Behovtype.Utbetaling
            detaljer()["maksdato"] er null
            detaljer()["saksbehandler"] er "Ola Nordmann"
            detaljer()["fagområde"] er "SPREF"
        }

        sjekkAt(speilApi().arbeidsgivere[0]) {
            vedtaksperioder[0].tilstand er TilstandstypeDTO.Annullert
        }
    }



    @Test
    fun `Ved feilet annulleringsutbetaling settes utbetaling til annullering feilet`() {
        håndterKansellerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        håndterUtbetalt(1.vedtaksperiode, status = UtbetalingHendelse.Oppdragstatus.FEIL, annullert = true)

        sjekkAt(inspektør) {
            personLogg.hasErrorsOrWorse() ellers personLogg.toString()
            arbeidsgiverOppdrag.size er 2
        }

        sjekkAt(speilApi().arbeidsgivere[0]) {
            vedtaksperioder[0].tilstand er TilstandstypeDTO.AnnulleringFeilet
        }
    }

    @Test
    fun `En enkel periode som er avsluttet som blir annullert blir også satt i tilstand TilAnnullering`() {
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(0))
        }
        håndterKansellerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteForkastetTilstand(0))
        }
    }

    @Test
    fun `Annullering av én periode fører til at alle avsluttede sammenhengende perioder blir satt i tilstand TilAnnullering`() {
        forlengVedtak(27.januar, 31.januar, 100)
        forlengPeriode(1.februar, 20.februar, 100)
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(0))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1))
            assertEquals(TilstandType.AVVENTER_HISTORIKK, inspektør.sisteTilstand(2))
        }
        håndterKansellerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteForkastetTilstand(0))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteForkastetTilstand(1))
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(2))
            assertTrue(it.utbetalinger.last().erAnnullert())
            assertFalse(it.utbetalinger.last().erUtbetalt())
        }
        håndterUtbetalt(1.vedtaksperiode, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT, annullert = true)
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteForkastetTilstand(0))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteForkastetTilstand(1))
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(2))
            assertEquals(Behovtype.Utbetaling, it.personLogg.behov().last().type)
            assertTrue(it.utbetalinger.last().erAnnullert())
            assertTrue(it.utbetalinger.last().erUtbetalt())
        }
    }

    @Test
    fun `Periode som håndterer godkjent annullering i TilAnnullering blir forkastet`() {
        håndterKansellerUtbetaling()
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteForkastetTilstand(0))
        }
        håndterUtbetalt(1.vedtaksperiode, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT, annullert = true)
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteForkastetTilstand(0))
        }
    }

    @Test
    fun `Periode som håndterer avvist annullering i TilAnnullering blir værende i TilAnnullering`() {
        håndterKansellerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteForkastetTilstand(0))
        }
        håndterUtbetalt(1.vedtaksperiode, status = UtbetalingHendelse.Oppdragstatus.AVVIST, annullert = true)
        inspektør.also {
            assertTrue(it.personLogg.hasErrorsOrWorse())
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteForkastetTilstand(0))
        }
    }

    @Disabled("Slik skal det virke etter at annullering trigger replay")
    @Test
    fun `Annullering av én periode fører kun til at sammehengende perioder blir satt i tilstand TilInfotrygd`() {
        forlengVedtak(27.januar, 30.januar, 100)
        nyttVedtak(1.mars, 20.mars, 100, 1.mars)
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(0))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(2))
        }
        val behovTeller = inspektør.personLogg.behov().size
        håndterKansellerUtbetaling(fagsystemId = inspektør.arbeidsgiverOppdrag.first().fagsystemId())
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertEquals(1, it.personLogg.behov().size - behovTeller, it.personLogg.toString())
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(1))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(0))
        }
    }

    @Test
    fun `publiserer et event ved annullering`() {
        håndterKansellerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        håndterUtbetalt(
            1.vedtaksperiode,
            status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
            saksbehandlerEpost = "tbd@nav.no",
            annullert = true
        )

        val annullering = observatør.annulleringer.lastOrNull()
        assertNotNull(annullering)

        assertEquals(inspektør.fagsystemId(1.vedtaksperiode), annullering!!.fagsystemId)

        val utbetalingslinje = annullering.utbetalingslinjer.first()
        assertEquals("tbd@nav.no", annullering.saksbehandlerEpost)
        assertEquals(19.januar, utbetalingslinje.fom)
        assertEquals(26.januar, utbetalingslinje.tom)
        assertEquals(8586, utbetalingslinje.beløp)
        assertEquals(100.0, utbetalingslinje.grad)
    }

    @Test
    fun `publiserer kun ett event ved annullering av utbetaling som strekker seg over flere vedtaksperioder`() {
        val fagsystemId = inspektør.fagsystemId(1.vedtaksperiode)
        forlengVedtak(27.januar, 20.februar, 100)
        assertEquals(2, observatør.vedtaksperioder.size)

        håndterKansellerUtbetaling(fagsystemId = fagsystemId)
        håndterUtbetalt(
            1.vedtaksperiode,
            status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
            saksbehandlerEpost = "tbd@nav.no",
            annullert = true
        )

        val vedtaksperioderIder = observatør.vedtaksperioder.toList()
        assertEquals(TilstandType.AVSLUTTET, observatør.tilstander[vedtaksperioderIder[0]]?.last())
        assertEquals(TilstandType.AVSLUTTET, observatør.tilstander[vedtaksperioderIder[1]]?.last())

        val annulleringer = observatør.annulleringer
        assertEquals(1, annulleringer.size)
        val annullering = annulleringer.lastOrNull()
        assertNotNull(annullering)

        assertEquals(fagsystemId, annullering!!.fagsystemId)

        val utbetalingslinje = annullering.utbetalingslinjer.first()
        assertEquals("tbd@nav.no", annullering.saksbehandlerEpost)
        assertEquals(19.januar, utbetalingslinje.fom)
        assertEquals(20.februar, utbetalingslinje.tom)
        assertEquals(32913, utbetalingslinje.beløp)
        assertEquals(100.0, utbetalingslinje.grad)
    }


    private inner class TestOppdragInspektør(oppdrag: Oppdrag) : OppdragVisitor {
        val oppdrag = mutableListOf<Oppdrag>()
        val linjer = mutableListOf<Utbetalingslinje>()
        val endringskoder = mutableListOf<Endringskode>()
        val fagsystemIder = mutableListOf<String?>()
        val refFagsystemIder = mutableListOf<String?>()

        init {
            oppdrag.accept(this)
        }

        override fun preVisitOppdrag(
            oppdrag: Oppdrag,
            totalBeløp: Int,
            nettoBeløp: Int,
            tidsstempel: LocalDateTime,
            utbetalingtilstand: Oppdrag.Utbetalingtilstand
        ) {
            this.oppdrag.add(oppdrag)
            fagsystemIder.add(oppdrag.fagsystemId())
        }

        override fun visitUtbetalingslinje(
            linje: Utbetalingslinje,
            fom: LocalDate,
            tom: LocalDate,
            beløp: Int?,
            aktuellDagsinntekt: Int,
            grad: Double,
            delytelseId: Int,
            refDelytelseId: Int?,
            refFagsystemId: String?,
            endringskode: Endringskode,
            datoStatusFom: LocalDate?
        ) {
            linjer.add(linje)
            endringskoder.add(endringskode)
            refFagsystemIder.add(refFagsystemId)
        }

    }
}
