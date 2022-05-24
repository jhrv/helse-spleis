package no.nav.helse.serde.api

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utdanning
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utlandsopphold
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.søppelbøtte
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Person
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.september
import no.nav.helse.serde.api.InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO
import no.nav.helse.serde.api.dto.EndringskodeDTO
import no.nav.helse.serde.api.v2.BeregnetPeriode
import no.nav.helse.serde.api.v2.Inntektkilde
import no.nav.helse.serde.api.v2.InntektsmeldingDTO
import no.nav.helse.serde.api.v2.SykmeldingDTO
import no.nav.helse.serde.api.v2.SøknadNavDTO
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterAnnullerUtbetaling
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.spleis.e2e.speilApi
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SpeilBuilderTest : AbstractEndToEndTest() {

    @Test
    fun `happy case`() {
        nyttVedtak(1.januar, 31.januar)
        val personDTO = speilApi()
        val førsteVedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO

        assertEquals(UNG_PERSON_FNR_2018.toString(), personDTO.fødselsnummer)
        assertEquals(1, personDTO.arbeidsgivere.size)
        assertNotNull(personDTO.versjon)
        assertEquals(1, førsteVedtaksperiode.beregningIder.size)
    }

    /**
     * Test for å verifisere at kontrakten mellom Spleis og Speil opprettholdes.
     * Hvis du trenger å gjøre endringer i denne testen må du sannsynligvis også gjøre endringer i Speil.
     */
    @Test
    fun `personDTO-en inneholder de feltene Speil forventer`() {
        val fom = 1.januar
        val tom = 31.januar

        val sykmeldingId = UUID.randomUUID()
        val sykmelding = SykmeldingDTO(sykmeldingId.toString(), 1.januar, 31.januar, 1.januar.atStartOfDay())
        val søknadId = UUID.randomUUID()
        val søknad = SøknadNavDTO(søknadId.toString(), 1.januar, 31.januar, 1.januar.atStartOfDay(), sendtNav = 1.februar.atStartOfDay())
        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding =
            InntektsmeldingDTO(inntektsmeldingId.toString(), mottattDato = 2.januar.atStartOfDay(), INNTEKT.reflection { _, månedlig, _, _ -> månedlig })

        val hendelser = listOf(sykmelding, søknad, inntektsmelding)

        håndterSykmelding(Sykmeldingsperiode(sykmelding.fom, sykmelding.tom, 100.prosent), id = sykmeldingId)
        håndterSøknad(Sykdom(søknad.fom, søknad.tom, 100.prosent), id = søknadId, sendtTilNAVEllerArbeidsgiver = 1.februar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), id = inntektsmeldingId)

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 1.januar(2017), null)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val personDTO = serializePersonForSpeil(person, hendelser)

        assertEquals(UNG_PERSON_FNR_2018.toString(), personDTO.fødselsnummer)
        assertEquals(AKTØRID, personDTO.aktørId)
        assertEquals(1, personDTO.arbeidsgivere.size)

        val arbeidsgiver = personDTO.arbeidsgivere.first()
        assertEquals(ORGNUMMER, arbeidsgiver.organisasjonsnummer)
        assertEquals(1, arbeidsgiver.vedtaksperioder.size)

        val vedtaksperiode = arbeidsgiver.vedtaksperioder.first() as VedtaksperiodeDTO
        assertEquals(1.januar, vedtaksperiode.fom)
        assertEquals(31.januar, vedtaksperiode.tom)
        assertEquals(TilstandstypeDTO.Utbetalt, vedtaksperiode.tilstand)
        assertTrue(vedtaksperiode.fullstendig)
        assertEquals(
            Grunnbeløp.`1G`.beløp(1.januar).reflection { årlig, _, _, _ -> årlig }.toInt(),
            vedtaksperiode.grunnbeløpgrense!!.beløp
        )
        assertEquals(
            Grunnbeløp.`6G`.beløp(1.januar).reflection { årlig, _, _, _ -> årlig },
            vedtaksperiode.grunnbeløpgrense!!.utregnet
        )
        assertEquals(
            Grunnbeløp.virkningstidspunktFor(Grunnbeløp.`1G`.beløp(1.januar)),
            vedtaksperiode.grunnbeløpgrense!!.virkningstidspunkt
        )

        val modellUtbetaling = inspektør.utbetalinger.first()
        val apiUtbetalinger = vedtaksperiode.utbetalinger
        assertEquals(
            modellUtbetaling.inspektør.arbeidsgiverOppdrag.fagsystemId(),
            apiUtbetalinger.arbeidsgiverUtbetaling!!.fagsystemId
        )
        assertEquals(
            modellUtbetaling.inspektør.personOppdrag.fagsystemId(),
            apiUtbetalinger.personUtbetaling!!.fagsystemId
        )
        assertEquals(
            modellUtbetaling.inspektør.arbeidsgiverOppdrag.førstedato,
            apiUtbetalinger.arbeidsgiverUtbetaling!!.linjer.first().fom
        )
        assertEquals(
            modellUtbetaling.inspektør.arbeidsgiverOppdrag.sistedato,
            apiUtbetalinger.arbeidsgiverUtbetaling!!.linjer.first().tom
        )

        val utbetalingstidslinje = vedtaksperiode.utbetalingstidslinje
        assertEquals(31, utbetalingstidslinje.size)
        assertEquals(DagtypeDTO.ArbeidsgiverperiodeDag, utbetalingstidslinje.first().type)
        assertEquals(DagtypeDTO.NavDag, utbetalingstidslinje.last().type)
        assertEquals(100.0, (utbetalingstidslinje.last() as NavDagDTO).grad)

        assertEquals(15741, vedtaksperiode.totalbeløpArbeidstaker)

        val sykdomstidslinje = vedtaksperiode.sykdomstidslinje
        assertEquals(31, sykdomstidslinje.size)
        assertEquals(SpeilDagtype.SYKEDAG, sykdomstidslinje.first().type)
        assertEquals(100.0, (sykdomstidslinje.last()).grad)
        assertEquals("Søknad", sykdomstidslinje.first().kilde.type.toString())
        assertEquals(1.januar, sykdomstidslinje.first().dagen)

        assertEquals("Ola Nordmann", vedtaksperiode.godkjentAv)

        val vilkår = vedtaksperiode.vilkår

        val sykepengedager = vilkår.sykepengedager
        assertEquals(11, sykepengedager.forbrukteSykedager)
        assertEquals(fom, sykepengedager.skjæringstidspunkt)
        assertEquals(fom.plusDays(16), sykepengedager.førsteSykepengedag)
        assertEquals(28.desember, sykepengedager.maksdato)
        assertEquals(237, sykepengedager.gjenståendeDager)
        assertTrue(sykepengedager.oppfylt)

        val alder = vilkår.alder
        assertEquals(25, alder.alderSisteSykedag)
        assertTrue(alder.oppfylt!!)

        val opptjening = vilkår.opptjening
        assertEquals(365, opptjening?.antallKjenteOpptjeningsdager)
        assertEquals(1.januar(2017), opptjening?.fom)
        assertTrue(opptjening?.oppfylt!!)

        val søknadsfrist = vilkår.søknadsfrist
        assertEquals(1.februar.atStartOfDay(), søknadsfrist?.sendtNav)
        assertEquals(fom, søknadsfrist?.søknadFom)
        assertEquals(tom, søknadsfrist?.søknadTom)
        assertTrue(søknadsfrist!!.oppfylt)

        val medlemskapstatus = vilkår.medlemskapstatus
        assertEquals(MedlemskapstatusDTO.JA, medlemskapstatus)

        assertEquals(31000.0, vedtaksperiode.inntektFraInntektsmelding)
        assertEquals(2, vedtaksperiode.hendelser.size)

        assertEquals(372000.0, vedtaksperiode.dataForVilkårsvurdering?.beregnetÅrsinntektFraInntektskomponenten)
        assertEquals(0.0, vedtaksperiode.dataForVilkårsvurdering?.avviksprosent)

        vedtaksperiode.simuleringsdata?.let { simulering ->
            assertNotNull(simulering.totalbeløp)
            simulering.perioder.assertOnNonEmptyCollection { periode ->
                assertNotNull(periode.fom)
                assertNotNull(periode.tom)
                periode.utbetalinger.assertOnNonEmptyCollection { utbetaling ->
                    assertNotNull(utbetaling.utbetalesTilNavn)
                    utbetaling.detaljer.assertOnNonEmptyCollection { detalj ->
                        assertNotNull(detalj.beløp)
                        assertNotNull(detalj.konto)
                        assertNotNull(detalj.sats)
                        assertTrue(detalj.klassekodeBeskrivelse.isNotEmpty())
                    }
                }
            }
        }
    }

    @Test
    fun `dager før skjæringstidspunkt og etter sisteSykedag skal kuttes vekk fra utbetalingstidslinje`() {
        nyttVedtak(1.januar, 31.januar)
        val personDTO = speilApi()
        val vedtaksperiodeDTO = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        assertEquals(1.januar, vedtaksperiodeDTO.utbetalingstidslinje.first().dato)
        assertEquals(31.januar, vedtaksperiodeDTO.utbetalingstidslinje.last().dato)
    }

    @Test
    fun `annullerer feilet revurdering`() {
        nyttVedtak(1.januar, 31.januar)

        håndterOverstyrTidslinje()
        håndterYtelser()
        håndterSimulering(simuleringOK = false)
        håndterAnnullerUtbetaling()

        val personDTO = speilApi()
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        val beregningIdVedtaksperiode = vedtaksperiode.beregningIder.last()
        val beregningIderUtbetalingshistorikk = personDTO.arbeidsgivere.first().utbetalingshistorikk.map { it.beregningId }

        assertEquals(1, vedtaksperiode.beregningIder.size)
        assertTrue(vedtaksperiode.fullstendig)
        assertEquals(2, beregningIderUtbetalingshistorikk.size)
        assertTrue(beregningIderUtbetalingshistorikk.contains(beregningIdVedtaksperiode))
    }

    @Test
    fun `kan mappe perioder som har beregning men står i avsluttet uten utbetaling`() {
        tilGodkjenning(1.januar, 19.januar, 100.prosent, 1.januar)
        håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(17.januar, Feriedag),
                ManuellOverskrivingDag(18.januar, Feriedag),
                ManuellOverskrivingDag(19.januar, Feriedag)
            )
        )
        håndterYtelser()

        val personDTO = speilApi()
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first()
        assertTrue(vedtaksperiode.fullstendig)
    }

    @Test
    fun `mapping av utbetalingshistorikk`() {
        nyttVedtak(1.januar, 31.januar)
        val personDTO = speilApi()
        val tidslinje = personDTO.arbeidsgivere.first().utbetalingshistorikk.first()
        val utbetaling = inspektør.utbetalinger.first()
        val vilkårsgrunnlagIder = inspektør.vilkårsgrunnlagHistorikkInnslag()

        assertEquals(1, personDTO.arbeidsgivere.single().utbetalingshistorikk.size)
        assertEquals(31, tidslinje.beregnettidslinje.size)
        assertEquals(16, tidslinje.hendelsetidslinje.size)
        assertEquals(31, tidslinje.utbetaling.utbetalingstidslinje.size)

        assertEquals(vilkårsgrunnlagIder[0].id, personDTO.arbeidsgivere.first().utbetalingshistorikk[0].vilkårsgrunnlagHistorikkId)
        assertEquals(Utbetalingstatus.UTBETALT, tidslinje.utbetaling.status)
        assertEquals(Utbetalingtype.UTBETALING, tidslinje.utbetaling.type)
        assertEquals(237, tidslinje.utbetaling.gjenståendeSykedager)
        assertEquals(11, tidslinje.utbetaling.forbrukteSykedager)
        assertEquals(15741, tidslinje.utbetaling.arbeidsgiverNettoBeløp)
        assertEquals(utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), tidslinje.utbetaling.arbeidsgiverFagsystemId)
        assertNotNull(tidslinje.utbetaling.vurdering)
        assertNotNull(tidslinje.tidsstempel)
        tidslinje.utbetaling.vurdering?.also {
            assertEquals("Ola Nordmann", it.ident)
            assertEquals(true, it.godkjent)
            assertEquals(false, it.automatisk)
            assertNotNull(it.tidsstempel)
        }
    }

    @Test
    fun `generer ett utbetalingshistorikkelement per utbetaling, selv om utbetalingene peker på samme sykdomshistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        //Spill igjennom første
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        //Spill igjennom andre
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        val personDTO = speilApi()
        assertEquals(2, personDTO.arbeidsgivere.first().utbetalingshistorikk.size)
        val arbeidsgiver = personDTO.arbeidsgivere.first()
        val førsteElement = arbeidsgiver.utbetalingshistorikk.first()
        val andreElement = arbeidsgiver.utbetalingshistorikk[1]

        assertEquals((arbeidsgiver.vedtaksperioder[1] as VedtaksperiodeDTO).beregningIder[0], førsteElement.beregningId)
        assertEquals((arbeidsgiver.vedtaksperioder.first() as VedtaksperiodeDTO).beregningIder[0], andreElement.beregningId)
        assertEquals(LocalDate.of(2018, 2, 28), førsteElement.utbetaling.utbetalingstidslinje.last().dato)
        assertEquals(LocalDate.of(2018, 1, 31), andreElement.utbetaling.utbetalingstidslinje.last().dato)
    }

    @Test
    fun `kobler beregningsId i vedtaksperioden til utbetalingshistorikken`() {
        nyttVedtak(1.januar, 31.januar)
        val personDTO = speilApi()

        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        assertEquals(1, vedtaksperiode.beregningIder.size)
        val utbetalingFraHistorikk = personDTO.arbeidsgivere.first().utbetalingshistorikk.first().utbetaling
        assertEquals(vedtaksperiode.beregningIder.first(), utbetalingFraHistorikk.beregningId)
        assertEquals(Utbetalingtype.UTBETALING, utbetalingFraHistorikk.type)
        assertEquals(28.desember, utbetalingFraHistorikk.maksdato)
        assertTrue(personDTO.arbeidsgivere.first().utbetalingshistorikk.first().beregningId in vedtaksperiode.beregningIder)
    }

    @Test
    fun `mapping av utbetalingshistorikk med flere perioder`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 14.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 14.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        val personDTO = speilApi()
        val nyesteHistorikkElement = personDTO.arbeidsgivere.first().utbetalingshistorikk.first()
        val eldsteHistorikkElement = personDTO.arbeidsgivere.first().utbetalingshistorikk.last()
        assertEquals(2, personDTO.arbeidsgivere.first().utbetalingshistorikk.size)
        assertEquals(1.februar, nyesteHistorikkElement.hendelsetidslinje.first().dagen)
        assertEquals(14.februar, nyesteHistorikkElement.hendelsetidslinje.last().dagen)
        assertEquals(1.januar, nyesteHistorikkElement.beregnettidslinje.first().dagen)
        assertEquals(14.februar, nyesteHistorikkElement.beregnettidslinje.last().dagen)

        assertEquals(1.januar, eldsteHistorikkElement.hendelsetidslinje.first().dagen)
        assertEquals(16.januar, eldsteHistorikkElement.hendelsetidslinje.last().dagen)
        assertEquals(1.januar, eldsteHistorikkElement.beregnettidslinje.first().dagen)
        assertEquals(31.januar, eldsteHistorikkElement.beregnettidslinje.last().dagen)

        assertNotNull(eldsteHistorikkElement.utbetaling.vurdering)
        assertNull(nyesteHistorikkElement.utbetaling.vurdering)
        eldsteHistorikkElement.utbetaling.vurdering?.also {
            assertEquals("Ola Nordmann", it.ident)
            assertEquals(true, it.godkjent)
            assertEquals(false, it.automatisk)
            assertNotNull(it.tidsstempel)
        }
    }

    @Test
    fun `person uten utbetalingsdager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 9.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 9.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        val personDTO = speilApi()
        assertEquals(TilstandstypeDTO.IngenUtbetaling, (personDTO.arbeidsgivere.first().vedtaksperioder.first()).tilstand)
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first()
        assertFalse(vedtaksperiode.fullstendig)
        assertEquals(0, vedtaksperiode.utbetalingstidslinje.size)
    }

    @Test
    fun `person med foreldet dager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.juni)
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        val personDTO = speilApi()

        assertEquals(1, personDTO.arbeidsgivere.first().vedtaksperioder.size)

        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first()
        assertFalse(vedtaksperiode.fullstendig)
        val utbetalingstidslinje = vedtaksperiode.utbetalingstidslinje
        assertTrue(utbetalingstidslinje.isEmpty())
        val sykdomstidslinje = vedtaksperiode.sykdomstidslinje
        assertEquals(SpeilDagtype.FORELDET_SYKEDAG, sykdomstidslinje.first().type)
        assertEquals(SpeilDagtype.FORELDET_SYKEDAG, sykdomstidslinje.last().type)
    }

    @Test
    fun `ufullstendig vedtaksperiode når tilstand er Venter`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        val personDTO = speilApi()

        val arbeidsgiver = personDTO.arbeidsgivere[0]
        val vedtaksperioder = arbeidsgiver.vedtaksperioder

        assertFalse(vedtaksperioder.first().fullstendig)
    }

    @Test
    fun `passer på at vedtakene har alle hendelsene`() {
        val sykmelding1Id = UUID.randomUUID()
        val sykmelding1 = SykmeldingDTO(sykmelding1Id.toString(), 1.januar, 31.januar, 1.januar.atStartOfDay())
        val søknad1Id = UUID.randomUUID()
        val søknad1 = SøknadNavDTO(søknad1Id.toString(), 1.januar, 31.januar, 1.januar.atStartOfDay(), 31.januar.atStartOfDay())
        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding =
            InntektsmeldingDTO(inntektsmeldingId.toString(), mottattDato = 2.januar.atStartOfDay(), INNTEKT.reflection { _, månedlig, _, _ -> månedlig })

        val sykmelding2Id = UUID.randomUUID()
        val sykmelding2 = SykmeldingDTO(sykmelding2Id.toString(), 1.februar, 14.februar, 1.februar.atStartOfDay())
        val søknad2Id = UUID.randomUUID()
        val søknad2 = SøknadNavDTO(søknad2Id.toString(), 1.februar, 14.februar, 1.februar.atStartOfDay(), 14.februar.atStartOfDay())

        val hendelser = listOf(sykmelding1, søknad1, inntektsmelding, sykmelding2, søknad2)

        håndterSykmelding(Sykmeldingsperiode(sykmelding1.fom, sykmelding1.tom, 100.prosent), id = sykmelding1Id)
        håndterSøknad(Sykdom(søknad1.fom, søknad1.tom, 100.prosent), id = søknad1Id)
        håndterInntektsmelding(listOf(1.januar til 16.januar), id = inntektsmeldingId)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(sykmelding2.fom, sykmelding2.tom, 100.prosent), id = sykmelding2Id)
        håndterSøknad(Sykdom(søknad2.fom, søknad2.tom, 100.prosent), id = søknad2Id)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val personDTO = speilApi(hendelser)

        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>()

        assertEquals(2, vedtaksperioder.size)
        assertEquals(2, vedtaksperioder.first().hendelser.size)
        assertEquals(2, vedtaksperioder.last().hendelser.size)
        assertEquals(inntektsmeldingId, vedtaksperioder.first().inntektsmeldingId)
        assertEquals(inntektsmeldingId, vedtaksperioder.last().inntektsmeldingId)
        assertTrue(vedtaksperioder.first().hendelser.map { UUID.fromString(it.id) }.containsAll(listOf(søknad1Id, inntektsmeldingId)))
        assertTrue(vedtaksperioder.last().hendelser.map { UUID.fromString(it.id) }.containsAll(listOf(søknad2Id, inntektsmeldingId)))
    }

    @Test
    fun `Utbetalinger blir lagt riktig på hver vedtaksperiode`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 14.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 14.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val personDTO = speilApi()
        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>()
        val utbetalinger = vedtaksperioder[1].utbetalteUtbetalinger

        val utbetalteUtbetalinger = inspektør.utbetalinger.filter { it.inspektør.erUtbetalt }
        assertEquals(
            utbetalteUtbetalinger.last().inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(),
            utbetalinger.arbeidsgiverUtbetaling!!.fagsystemId
        )
        assertTrue(utbetalinger.personUtbetaling!!.linjer.isEmpty())
        assertEquals(
            utbetalteUtbetalinger.last().inspektør.arbeidsgiverOppdrag.førstedato,
            utbetalinger.arbeidsgiverUtbetaling!!.linjer.first().fom
        )
        assertEquals(
            utbetalteUtbetalinger.last().inspektør.arbeidsgiverOppdrag.sistedato,
            utbetalinger.arbeidsgiverUtbetaling!!.linjer.first().tom
        )
    }

    @Test
    fun `passer på at alle vedtak får fellesdata for sykefraværet`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 14.februar)
        nyttVedtak(20.februar, 28.februar)

        val personDTO = speilApi()

        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>()

        assertEquals(3, vedtaksperioder.size)
        assertEquals(vedtaksperioder.first().gruppeId, vedtaksperioder[1].gruppeId)
        assertNotEquals(vedtaksperioder.first().gruppeId, vedtaksperioder.last().gruppeId)

        assertNotNull(vedtaksperioder.first().dataForVilkårsvurdering)
        assertNotNull(vedtaksperioder.first().vilkår.opptjening)
        assertEquals(vedtaksperioder.first().dataForVilkårsvurdering, vedtaksperioder[1].dataForVilkårsvurdering)
        assertEquals(vedtaksperioder.first().vilkår.opptjening, vedtaksperioder[1].vilkår.opptjening)

        assertEquals(220, vedtaksperioder.last().vilkår.sykepengedager.gjenståendeDager)
        assertEquals(28, vedtaksperioder.last().vilkår.sykepengedager.forbrukteSykedager)
        assertEquals(2.januar(2019), vedtaksperioder.last().vilkår.sykepengedager.maksdato)
    }

    @Test
    fun `forlengelse fra Infotrygd får riktig skjæringstidspunkt`() {
        val fom1Periode = 1.januar
        val tom1Periode = 31.januar
        val skjæringstidspunktFraInfotrygd = 1.desember(2017)
        val fom2Periode = 1.februar
        val tom2Periode = 14.februar
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, skjæringstidspunktFraInfotrygd, INNTEKT, true))

        håndterSykmelding(Sykmeldingsperiode(fom1Periode, tom1Periode, 100.prosent))
        håndterSøknad(Sykdom(fom1Periode, tom1Periode, 100.prosent))
        // Til infotrygd pga overlapp
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, skjæringstidspunktFraInfotrygd, 4.januar, 100.prosent, INNTEKT),
            inntektshistorikk = inntektshistorikk,
            besvart = 16.februar.atStartOfDay()
        )

        håndterSykmelding(Sykmeldingsperiode(fom2Periode, tom2Periode, 100.prosent))
        håndterSøknad(Sykdom(fom2Periode, tom2Periode, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, skjæringstidspunktFraInfotrygd, tom1Periode, 100.prosent, INNTEKT),
            inntektshistorikk = inntektshistorikk
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val personDTO = speilApi()

        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>()
            .also {
                assertEquals(1, it.size)
            }

        // Denne periode er forlengelse av Infotrygd-periode.
        assertEquals(Periodetype.OVERGANG_FRA_IT, vedtaksperioder.first().periodetype)
    }

    @Test
    fun `overgang fra infotrygd får ikke riktig periodetype ved forkasting`() {
        val fom1Periode = 1.januar
        val tom1Periode = 31.januar
        val skjæringstidspunktFraInfotrygd = 1.desember(2017)
        val fom2Periode = 1.februar
        val tom2Periode = 14.februar

        håndterSykmelding(Sykmeldingsperiode(fom1Periode, tom1Periode, 100.prosent))
        håndterSøknad(Sykdom(fom1Periode, tom1Periode, 100.prosent))

        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, skjæringstidspunktFraInfotrygd, 31.desember(2017), 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, skjæringstidspunktFraInfotrygd, INNTEKT, true))
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(fom2Periode, tom2Periode, 100.prosent))
        håndterSøknad(Sykdom(fom2Periode, tom2Periode, 100.prosent))
        person.søppelbøtte(hendelselogg, inspektør.periode(2.vedtaksperiode))

        val personDTO = speilApi()

        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>()
            .also {
                assertEquals(1, it.size)
            }

        assertEquals(Periodetype.OVERGANG_FRA_IT, vedtaksperioder.first().periodetype)
    }

    @Test
    fun `hvis første vedtaksperiode er ferdigbehandlet arbeidsgiverperiode vises den som ferdigbehandlet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 9.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 9.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 25.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        val personDTO = speilApi()

        val vedtaksperiodeDTO = personDTO.arbeidsgivere[0].vedtaksperioder[1] as VedtaksperiodeDTO
        assertNotNull(vedtaksperiodeDTO.dataForVilkårsvurdering)
        assertNotNull(vedtaksperiodeDTO.vilkår.opptjening)
        assertFalse(personDTO.arbeidsgivere[0].vedtaksperioder[0].fullstendig)
        assertTrue(personDTO.arbeidsgivere[0].vedtaksperioder[1].fullstendig)
    }

    @Test
    fun `perioder uten utbetaling får ikke utbetalingstidslinje`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 9.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 9.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 25.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        val personDTO = speilApi()
        assertEquals(0, personDTO.arbeidsgivere.first().vedtaksperioder.first().utbetalingstidslinje.size)
    }

    @Test
    fun `null gjenstående dager ved oppnådd maksdato`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        forlengVedtak(1.april, 30.april)
        forlengVedtak(1.mai, 31.mai)
        forlengVedtak(1.juni, 30.juni)
        forlengVedtak(1.juli, 31.juli)
        forlengVedtak(1.august, 31.august)
        forlengVedtak(1.september, 30.september)
        forlengVedtak(1.oktober, 31.oktober)
        forlengVedtak(1.november, 30.november)
        forlengVedtak(1.desember, 31.desember) //Maksdato nådd

        håndterSykmelding(Sykmeldingsperiode(1.januar(2019), 31.januar(2019), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2019), 31.januar(2019), 100.prosent))
        håndterYtelser(13.vedtaksperiode)
        håndterUtbetalingsgodkjenning(13.vedtaksperiode)
        håndterUtbetalt()

        val personDTO = speilApi()
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(0, vedtaksperiode.vilkår.sykepengedager.gjenståendeDager)
    }

    @Test
    fun `ta med personoppdrag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            refusjon = Inntektsmelding.Refusjon(0.månedlig, null),
            førsteFraværsdag = 1.januar,
            arbeidsgiverperioder = listOf(1.januar til 16.januar)
        )
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        val personDTO = speilApi()
        val utbetalingstidslinje = personDTO.arbeidsgivere[0].utbetalingshistorikk[0].utbetaling.utbetalingstidslinje

        assertEquals(0, (personDTO.arbeidsgivere[0].vedtaksperioder[0] as VedtaksperiodeDTO).utbetalinger.arbeidsgiverUtbetaling?.linjer?.size)
        assertEquals(1, (personDTO.arbeidsgivere[0].vedtaksperioder[0] as VedtaksperiodeDTO).utbetalinger.personUtbetaling?.linjer?.size)

        utbetalingstidslinje.filterIsInstance<IkkeUtbetaltDagDTO>().let {
            it.forEach { arbeidsgiverperiodedag ->
                assertEquals(1431, arbeidsgiverperiodedag.inntekt)
                assertEquals(DagtypeDTO.ArbeidsgiverperiodeDag, arbeidsgiverperiodedag.type)
            }
            assertEquals(16, it.size)
        }

        utbetalingstidslinje.filterIsInstance<NavDagDTO>().let {
            it.forEach { navdag ->
                assertEquals(DagtypeDTO.NavDag, navdag.type)
                assertEquals(0, navdag.utbetaling)
                assertEquals(0, navdag.arbeidsgiverbeløp)
                assertEquals(1431, navdag.personbeløp)
                assertEquals(0, navdag.refusjonsbeløp)
            }
            assertEquals(11, it.size)
        }

        utbetalingstidslinje.filterIsInstance<NavHelgedagDTO>().let {
            it.forEach { navhelgedag ->
                assertEquals(DagtypeDTO.NavHelgDag, navhelgedag.type)
                assertEquals(100.0, navhelgedag.grad)
            }
            assertEquals(4, it.size)
        }

        assertEquals(0, personDTO.arbeidsgivere[0].utbetalingshistorikk[0].utbetaling.arbeidsgiverNettoBeløp)
        assertEquals(15741, personDTO.arbeidsgivere[0].utbetalingshistorikk[0].utbetaling.personNettoBeløp)
    }

    @Test
    fun `Skal ta med forkastede vedtaksperioder`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false) // Avvist av saksbehandler

        nyttVedtak(1.mars, 31.mars)

        val personDTO = speilApi()
        assertEquals(2, personDTO.arbeidsgivere.first().vedtaksperioder.size)
    }

    @Test
    fun `Skal ta med annullerte vedtaksperioder`() {
        nyttVedtak(1.januar, 31.januar)
        håndterAnnullerUtbetaling()

        val personDTO = speilApi()
        assertEquals(1, personDTO.arbeidsgivere.first().vedtaksperioder.size)
        assertEquals(TilstandstypeDTO.TilAnnullering, personDTO.arbeidsgivere[0].vedtaksperioder[0].tilstand)
    }

    @Test
    fun `lager ikke utbetalingshistorikkelement av forkastet utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterOverstyrTidslinje() // Overstyring forkaster utbetaling og sender periode tilbake

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val personDTO = speilApi()
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        val utbetalingshistorikk = personDTO.arbeidsgivere.first().utbetalingshistorikk

        assertEquals(31, vedtaksperiode.utbetalingstidslinje.size)
        assertEquals(1, vedtaksperiode.beregningIder.size)
        assertEquals(1, utbetalingshistorikk.size)
        assertEquals(vedtaksperiode.beregningIder.first(), utbetalingshistorikk.first().beregningId)
        assertEquals(Utbetalingstatus.IKKE_UTBETALT, utbetalingshistorikk.first().utbetaling.status)
    }

    @Test
    fun `Setter tilstand Utbetalt på vedtaksperioden uavhengig av utbetaling i påfølgende vedtaksperioder`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 10.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 10.februar, 100.prosent), Arbeid(1.februar, 10.februar))

        håndterYtelser(2.vedtaksperiode)

        val personDTO = speilApi()
        assertEquals(TilstandstypeDTO.Utbetalt, personDTO.arbeidsgivere[0].vedtaksperioder[0].tilstand)
    }

    @Test
    fun `Sender unike advarsler per periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2018), 31.januar(2018), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2018), 31.januar(2018), 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.april)
        håndterInntektsmelding(listOf(1.januar(2018) til 16.januar(2018)))

        håndterYtelser(1.vedtaksperiode, arbeidsavklaringspenger = listOf(1.januar(2018).minusDays(60) til 31.januar(2018).minusDays(60)))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, arbeidsavklaringspenger = listOf(1.januar(2018).minusDays(60) til 31.januar(2018).minusDays(60)))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        val personDTO = speilApi()
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(vedtaksperiode.aktivitetslogg.distinctBy { it.melding }, vedtaksperiode.aktivitetslogg)
    }

    @Test
    fun `Sender med varsler for tidligere periode som er avsluttet uten utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 9.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 9.januar, 100.prosent), Utdanning(3.januar, 4.januar))

        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 25.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        val personDTO = speilApi()
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(1, vedtaksperiode.aktivitetslogg.size)
        assertNotEquals(vedtaksperiode.id, vedtaksperiode.aktivitetslogg[0].vedtaksperiodeId)
    }

    @Test
    fun `Sender med varsler for alle tidligere tilstøtende perioder som er avsluttet uten utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 9.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 9.januar, 100.prosent), Utdanning(3.januar, 4.januar)) // Warning

        håndterSykmelding(Sykmeldingsperiode(10.januar, 14.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 14.januar, 100.prosent), Utlandsopphold(11.januar, 12.januar)) // Warning
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(15.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(15.januar, 25.januar, 100.prosent))

        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        val personDTO = speilApi()
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(2, vedtaksperiode.aktivitetslogg.size)
        assertEquals(
            "Utdanning oppgitt i perioden i søknaden.",
            vedtaksperiode.aktivitetslogg[0].melding
        )
        assertEquals(
            "Utenlandsopphold oppgitt i perioden i søknaden.",
            vedtaksperiode.aktivitetslogg[1].melding
        )
        assertNotEquals(vedtaksperiode.id, vedtaksperiode.aktivitetslogg[0].vedtaksperiodeId)
        assertNotEquals(vedtaksperiode.id, vedtaksperiode.aktivitetslogg[1].vedtaksperiodeId)
    }

    @Test
    fun `legger ved kildeId sammen med dag i tidslinja`() {
        val søknadNavDTO = SøknadNavDTO(UUID.randomUUID().toString(), 1.januar, 31.januar, 1.januar.atStartOfDay(), sendtNav = 31.januar.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), id = UUID.fromString(søknadNavDTO.id))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val personDTO = speilApi(listOf(søknadNavDTO))
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[0] as VedtaksperiodeDTO
        assertEquals(søknadNavDTO.id, vedtaksperiode.sykdomstidslinje[0].kilde.kildeId.toString())
    }

    @Test
    fun `egen tilstandstype for perioder med kun fravær - feriedager`()  {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 24.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 24.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent), Ferie(25.januar, 31.januar))
        håndterYtelser(2.vedtaksperiode)

        val personDTO = serializePersonForSpeil(person)
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[1] as VedtaksperiodeDTO
        assertTrue(vedtaksperiode.fullstendig)
        assertEquals(TilstandstypeDTO.Oppgaver, vedtaksperiode.tilstand)
    }

    @Test
    fun `egen tilstandstype for perioder med kun fravær - permisjonsdager (gir warning)`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 24.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 24.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent), Permisjon(25.januar, 31.januar))
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val personDTO = serializePersonForSpeil(person)
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[1] as VedtaksperiodeDTO
        assertTrue(vedtaksperiode.fullstendig)
        assertEquals(TilstandstypeDTO.KunFerie, vedtaksperiode.tilstand)
    }

    @Test
    fun `perioder med søknad arbeidsgiver blir ufullstendig`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        val personDTO = serializePersonForSpeil(person)
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[0] as UfullstendigVedtaksperiodeDTO
        assertFalse(vedtaksperiode.fullstendig)
        assertEquals(TilstandstypeDTO.IngenUtbetaling, vedtaksperiode.tilstand)
    }

    @Test
    fun `ny inntekt inkluderes`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val personDTO = serializePersonForSpeil(person)
        assertEquals(1, personDTO.arbeidsgivere.size)
        assertEquals(1, personDTO.arbeidsgivere[0].vedtaksperioder.size)
        val inntektsgrunnlag = personDTO.inntektsgrunnlag.find { it.skjæringstidspunkt == 1.januar }
        assertEquals(31000.0 * 12, inntektsgrunnlag?.sykepengegrunnlag)
        assertEquals(31000.0 * 12, inntektsgrunnlag?.omregnetÅrsinntekt)
        assertEquals(31000.0 * 12, inntektsgrunnlag?.sammenligningsgrunnlag)
        assertEquals(0.0, inntektsgrunnlag?.avviksprosent)
        assertEquals(31000.0 * 12 / 260, inntektsgrunnlag?.maksUtbetalingPerDag)
        assertEquals(1, inntektsgrunnlag?.inntekter?.size)
        inntektsgrunnlag?.inntekter?.forEach { arbeidsgiverinntekt ->
            assertEquals(ORGNUMMER, arbeidsgiverinntekt.arbeidsgiver)

            assertEquals(InntektkildeDTO.Inntektsmelding, arbeidsgiverinntekt.omregnetÅrsinntekt?.kilde)
            assertEquals(31000.0 * 12, arbeidsgiverinntekt.omregnetÅrsinntekt?.beløp)
            assertEquals(31000.0, arbeidsgiverinntekt.omregnetÅrsinntekt?.månedsbeløp)
            assertNull(arbeidsgiverinntekt.omregnetÅrsinntekt?.inntekterFraAOrdningen)

            assertEquals(31000.0 * 12, arbeidsgiverinntekt.sammenligningsgrunnlag?.beløp)
            assertEquals(12, arbeidsgiverinntekt.sammenligningsgrunnlag?.inntekterFraAOrdningen?.size)
            arbeidsgiverinntekt.sammenligningsgrunnlag?.inntekterFraAOrdningen?.forEachIndexed { index, inntekterFraAOrdningen ->
                assertEquals(YearMonth.of(2017, index + 1), inntekterFraAOrdningen.måned)
                assertEquals(31000.0, inntekterFraAOrdningen.sum)
            }
        }
    }

    @Test
    fun `legger ved felt for automatisk behandling for riktig periode`() {
        val fom = 1.januar
        val tom = 31.januar
        val forlengelseFom = 1.februar
        val forlengelseTom = 15.februar

        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        håndterSøknad(Sykdom(fom, tom, 100.prosent))
        håndterInntektsmelding(listOf(fom til fom.plusDays(15)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, automatiskBehandling = true)
        håndterUtbetalt()

        val personDTO = serializePersonForSpeil(person)
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[0] as VedtaksperiodeDTO
        assertTrue(vedtaksperiode.automatiskBehandlet)
        assertNotNull(vedtaksperiode.godkjentAv)

        håndterSykmelding(Sykmeldingsperiode(forlengelseFom, forlengelseTom, 100.prosent))
        håndterSøknad(Sykdom(forlengelseFom, forlengelseTom, 100.prosent), sendtTilNAVEllerArbeidsgiver = forlengelseFom.plusDays(1))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        val forlengelsePersonDTO = serializePersonForSpeil(person)
        val forlengelse = forlengelsePersonDTO.arbeidsgivere[0].vedtaksperioder[1] as VedtaksperiodeDTO
        assertFalse(forlengelse.automatiskBehandlet)
    }

    @Test
    fun `Total sykdomsgrad ved en arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val vedtaksperiodeDTO = serializePersonForSpeil(person)
            .arbeidsgivere.first()
            .vedtaksperioder.first() as VedtaksperiodeDTO

        assertEquals(100.0, vedtaksperiodeDTO.utbetalingstidslinje.filterIsInstance<NavDagDTO>().first().totalGrad)
    }

    @Test
    fun `markerer forkastede vedtaksperioder som forkastet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        // forkast periode
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))

        val serialisertPerson = serializePersonForSpeil(person)
        val vedtaksperiode = serialisertPerson.arbeidsgivere.first().vedtaksperioder.first()
        assertTrue(vedtaksperiode.erForkastet)
    }

    @Test
    fun `markerer vedtaksperioder som ikke forkastet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val serialisertPerson = serializePersonForSpeil(person)
        val vedtaksperiode = serialisertPerson.arbeidsgivere.first().vedtaksperioder.first()
        assertFalse(vedtaksperiode.erForkastet)
    }

    @Test
    fun `arbeidsgivere uten vedtaksperioder som skal vises i speil, filtreres bort`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)
        assertTrue(serializePersonForSpeil(person).arbeidsgivere.isEmpty())
    }

    @Test
    fun `Dødsdato ligger på person`() {
        val fom = 1.januar
        val tom = 31.januar
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1))
        håndterInntektsmelding(listOf(fom til fom.plusDays(15)))
        håndterYtelser(1.vedtaksperiode, dødsdato = 1.januar)

        assertEquals(1.januar, serializePersonForSpeil(person).dødsdato)
    }

    @Test
    fun `Forlengelse får ikke med warnings fra vilkårsprøving gjort i forrige periode`() {
        val fom = 1.januar
        val tom = 31.januar
        val forlengelseFom = 1.februar
        val forlengelseTom = 28.februar

        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(fom til fom.plusDays(15)),
            refusjon = Inntektsmelding.Refusjon(beløp = 1000.månedlig, opphørsdato = null, endringerIRefusjon = emptyList()),
            beregnetInntekt = 1000.månedlig
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt 1000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, automatiskBehandling = false)

        håndterSykmelding(Sykmeldingsperiode(forlengelseFom, forlengelseTom, 100.prosent))
        håndterSøknad(Sykdom(forlengelseFom, forlengelseTom, 100.prosent), sendtTilNAVEllerArbeidsgiver = forlengelseTom)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, automatiskBehandling = false)

        val vedtaksperioder = serializePersonForSpeil(person)
            .arbeidsgivere.flatMap { it.vedtaksperioder }
        assertEquals(2, vedtaksperioder.size)
        assertTrue((vedtaksperioder.first() as VedtaksperiodeDTO).aktivitetslogg.any { it.melding == "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag" })
        assertFalse((vedtaksperioder.last() as VedtaksperiodeDTO).aktivitetslogg.any { it.melding == "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag" })
    }

    @Test
    fun `Vedtaksperioder fra flere arbeidsgivere får ikke samme vilkårsgrunnlag-warnings`() {
        val fom = 1.januar
        val tom = 31.januar

        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1), orgnummer = a1)
        håndterSøknad(Sykdom(fom, tom, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(fom til fom.plusDays(15)),
            refusjon = Inntektsmelding.Refusjon(beløp = 1000.månedlig, opphørsdato = null, endringerIRefusjon = emptyList()),
            beregnetInntekt = 1000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(fom til fom.plusDays(15)),
            refusjon = Inntektsmelding.Refusjon(beløp = 1000.månedlig, opphørsdato = null, endringerIRefusjon = emptyList()),
            beregnetInntekt = 1000.månedlig,
            orgnummer = a2
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 1000.månedlig
                    a2 inntekt 1000.månedlig
                }
            }),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, automatiskBehandling = false, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, automatiskBehandling = false, orgnummer = a2)

        val vedtaksperioder = serializePersonForSpeil(person)
            .arbeidsgivere.flatMap { it.vedtaksperioder }

        assertEquals(2, vedtaksperioder.size)
        assertTrue((vedtaksperioder.first() as VedtaksperiodeDTO).aktivitetslogg.any { it.melding == "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag" })
        assertFalse((vedtaksperioder.last() as VedtaksperiodeDTO).aktivitetslogg.any { it.melding == "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag" })
    }

    @Test
    fun `Akkumulerer inntekter fra a-orningen pr måned`() {
        val fom = 1.januar
        val tom = 31.januar

        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1), orgnummer = a1)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(fom til fom.plusDays(15)),
            refusjon = Inntektsmelding.Refusjon(beløp = 1000.månedlig, opphørsdato = null, endringerIRefusjon = emptyList()),
            beregnetInntekt = 1000.månedlig,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 1000.månedlig
                    a2 inntekt 600.månedlig
                    a2 inntekt 400.månedlig
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 1000.månedlig
                    a2 inntekt 600.månedlig
                    a2 inntekt 400.månedlig
                }
            }, arbeidsforhold = emptyList()),
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH), Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH)),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, automatiskBehandling = false)

        val personForSpeil = serializePersonForSpeil(person)
        val inntekterFraAOrdningenFraVilkårsgrunnlag = personForSpeil
            .vilkårsgrunnlagHistorikk
            .values
            .first()
            .values
            .first()
            .inntekter
            .first { it.organisasjonsnummer == a2 }
            .omregnetÅrsinntekt!!
            .inntekterFraAOrdningen!!
        assertEquals(3, inntekterFraAOrdningenFraVilkårsgrunnlag.size)
        assertTrue(inntekterFraAOrdningenFraVilkårsgrunnlag.all { it.sum == 1000.0 })

        val inntekterFraAOrdningenFraInntektsgrunnlag = personForSpeil
            .inntektsgrunnlag
            .first()
            .inntekter
            .first { it.arbeidsgiver == a2 }
            .omregnetÅrsinntekt!!
            .inntekterFraAOrdningen!!
        assertEquals(3, inntekterFraAOrdningenFraInntektsgrunnlag.size)
        assertTrue(inntekterFraAOrdningenFraInntektsgrunnlag.all { it.sum == 1000.0 })
    }

    @Test
    fun `Begge arbeidsgivere har beregningsId og tilsvarende utbetalingshistorikkelement når første sendes til godkjenning`() {
        val fom = 1.januar
        val tom = 31.januar

        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1), orgnummer = a1)
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1), orgnummer = a2)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(fom til fom.plusDays(15)),
            refusjon = Inntektsmelding.Refusjon(beløp = 31000.månedlig, opphørsdato = null, endringerIRefusjon = emptyList()),
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(fom til fom.plusDays(15)),
            refusjon = Inntektsmelding.Refusjon(beløp = 31000.månedlig, opphørsdato = null, endringerIRefusjon = emptyList()),
            beregnetInntekt = 31000.månedlig,
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 31000.månedlig
                    a2 inntekt 31000.månedlig
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 31000.månedlig
                    a2 inntekt 31000.månedlig
                }
            }, arbeidsforhold = emptyList()),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertBeregningsider(person)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, automatiskBehandling = false, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)

        assertBeregningsider(person)
    }

    @Test
    fun `tar med annulleringer som separate historikkelementer()`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(2.vedtaksperiode))
        val personDto = serializePersonForSpeil(person)
        val utbetalingshistorikk = personDto.arbeidsgivere.first().utbetalingshistorikk
        assertEquals(3, utbetalingshistorikk.size)
        val annulleringElement = utbetalingshistorikk.first { it.utbetaling.erAnnullering() }
        assertFalse(personDto.arbeidsgivere.flatMap { it.vedtaksperioder.flatMap { vedtaksperiode -> (vedtaksperiode as VedtaksperiodeDTO).beregningIder } }
            .contains(annulleringElement.beregningId))
    }

    @Test
    fun `Flere arbeidsgivere med ghosts`() {
        val a5 = "567891234"
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        val gamleITPerioder = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a5, 1.januar(2009), 31.januar(2009), 100.prosent, 20000.månedlig)
        )
        val gamleITInntekter = listOf(Inntektsopplysning(a5, 1.januar(2009), 20000.månedlig, true))
        håndterYtelser(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = a1,
            utbetalinger = gamleITPerioder,
            inntektshistorikk = gamleITInntekter,
            besvart = LocalDateTime.MIN
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 1000.månedlig.repeat(2))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
                    grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), listOf(31000.månedlig, 32000.månedlig, 33000.månedlig)),
                    grunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 1000.månedlig.repeat(2)),
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a3, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a4, LocalDate.EPOCH, 1.desember(2017))
            )
        )
        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val personDto = serializePersonForSpeil(person)
        val inntekterFraAordningen = personDto.inntektsgrunnlag
            .single().inntekter
            .single { it.arbeidsgiver == a2 }.omregnetÅrsinntekt!!.inntekterFraAOrdningen!!

        assertEquals(listOf(a1, a2, a4).map(String::toString), personDto.arbeidsgivere.map { it.organisasjonsnummer })
        assertEquals(listOf(a1, a2, a4).map(String::toString), personDto.inntektsgrunnlag.single().inntekter.map { it.arbeidsgiver })
        assertEquals(3, inntekterFraAordningen.size)
        assertEquals(listOf(33000.0, 32000.0, 31000.0), inntekterFraAordningen.map { it.sum })
    }

    @Test
    fun `Viser inntektsgrunnlag for arbeidsforhold som startet innen 3 måneder før skjæringstidspunktet, selvom vi ikke har inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        håndterYtelser(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = a1,
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 25.november(2017), null),
            )
        )
        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val personDto = serializePersonForSpeil(person)

        assertEquals(listOf(a1, a2).map(String::toString), personDto.arbeidsgivere.map { it.organisasjonsnummer })
        assertEquals(listOf(a1, a2).map(String::toString), personDto.inntektsgrunnlag.single().inntekter.map { it.arbeidsgiver })

        val arbeidsgiverInntektA2 = personDto.vilkårsgrunnlagHistorikk.values.last()[1.januar]?.inntekter?.first { it.organisasjonsnummer == a2 }

        assertEquals(0.0, arbeidsgiverInntektA2?.omregnetÅrsinntekt?.beløp)
        assertEquals(Inntektkilde.IkkeRapportert, arbeidsgiverInntektA2?.omregnetÅrsinntekt?.kilde)
    }

    @Test
    fun `tar med arbeidsforhold som gjelder skjæringstidspunktet`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)

        val personDto = serializePersonForSpeil(person)
        assertEquals(listOf(ORGNUMMER), personDto.arbeidsforholdPerSkjæringstidspunkt[1.januar]?.map { it.orgnummer })
    }

    @Test
    fun `tar med refusjonshistorikk pr arbeidsgiver`() {
        nyttVedtak(
            fom = 1.januar,
            tom = 31.januar,
            grad = 100.prosent,
            refusjon = Inntektsmelding.Refusjon(
                beløp = INNTEKT,
                opphørsdato = null,
                endringerIRefusjon = listOf(
                    Inntektsmelding.Refusjon.EndringIRefusjon(beløp = INNTEKT.plus(1000.månedlig), 19.januar),
                    Inntektsmelding.Refusjon.EndringIRefusjon(beløp = INNTEKT.plus(2000.månedlig), 23.januar),
                )
            )
        )

        val personDto = speilApi()
        val periode: BeregnetPeriode = personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode
        val refusjon = requireNotNull(periode.refusjon)

        assertEquals(2, refusjon.endringer.size)
        assertEquals(32000.0, refusjon.endringer.first().beløp)
        assertEquals(19.januar, refusjon.endringer.first().dato)
        assertEquals(33000.0, refusjon.endringer.last().beløp)
        assertEquals(23.januar, refusjon.endringer.last().dato)
    }

    @Test
    fun `endringskode på oppdragslinjer`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, 23.januar))
        assertEndringskoder(arbeidsgiverEndringskode = EndringskodeDTO.NY, personEndringskode = EndringskodeDTO.NY)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(26.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertEndringskoder(arbeidsgiverEndringskode = EndringskodeDTO.UEND, personEndringskode = EndringskodeDTO.ENDR)
    }

    private fun assertEndringskoder(arbeidsgiverEndringskode: EndringskodeDTO, personEndringskode: EndringskodeDTO) {
        val personDto = serializePersonForSpeil(person)
        val vedtaksperiode = personDto.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        val beregnetPeriodetype = personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode
        assertEquals(arbeidsgiverEndringskode, vedtaksperiode.sisteUtbetaling!!.arbeidsgiverOppdrag.utbetalingslinjer.first().endringskode)
        assertEquals(arbeidsgiverEndringskode, beregnetPeriodetype.utbetaling.oppdrag.getValue(beregnetPeriodetype.utbetaling.arbeidsgiverFagsystemId).utbetalingslinjer.first().endringskode)
        assertEquals(personEndringskode, vedtaksperiode.sisteUtbetaling!!.personOppdrag.utbetalingslinjer.first().endringskode)
        assertEquals(personEndringskode, beregnetPeriodetype.utbetaling.oppdrag.getValue(beregnetPeriodetype.utbetaling.personFagsystemId).utbetalingslinjer.first().endringskode)
    }

    private fun <T> Collection<T>.assertOnNonEmptyCollection(func: (T) -> Unit) {
        assertTrue(isNotEmpty())
        forEach(func)
    }

    private fun assertBeregningsider(person: Person) {
        val personDTO = serializePersonForSpeil(person)
        val arbeidsgiver1 = personDTO.arbeidsgivere.first()
        val arbeidsgiver2 = personDTO.arbeidsgivere.last()
        val vedtaksperiode1 = arbeidsgiver1.vedtaksperioder.first() as VedtaksperiodeDTO
        val vedtaksperiode2 = arbeidsgiver2.vedtaksperioder.first() as VedtaksperiodeDTO

        assertEquals(1, vedtaksperiode1.beregningIder.size)
        assertEquals(1, vedtaksperiode2.beregningIder.size)

        assertTrue(arbeidsgiver1.utbetalingshistorikk.map { it.beregningId }.contains(vedtaksperiode1.beregningIder.first()))
        assertTrue(arbeidsgiver2.utbetalingshistorikk.map { it.beregningId }.contains(vedtaksperiode2.beregningIder.first()))
    }
}
