package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Dødsinfo
import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SimuleringHendelseTest : AbstractPersonTest() {
    private companion object {
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 28.februar
    }

    private lateinit var hendelse: ArbeidstakerHendelse

    @Test
    fun `simulering er OK`() {
        håndterYtelser()
        håndterSimuleringer()
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertFalse(person.personLogg.hasWarningsOrWorse())
    }

    @Test
    fun `simulering er ikke OK`() {
        håndterYtelser()
        håndterSimuleringer(mapOf(Fagområde.SykepengerRefusjon to Pair(false, 1431)))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `simulering ved delvis refusjon`() {
        håndterYtelser(Inntektsmelding.Refusjon(31000.månedlig, sisteSykedag.minusDays(7), emptyList()))
        håndterSimuleringer(mapOf(
            Fagområde.SykepengerRefusjon to Pair(true, 1431),
            Fagområde.Sykepenger to Pair(true, 1431)
        ))
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertFalse(person.personLogg.hasWarningsOrWorse())
    }

    @Test
    fun `simulering ved delvis refusjon hvor vi avventer en simulering`() {
        håndterYtelser(Inntektsmelding.Refusjon(31000.månedlig, sisteSykedag.minusDays(7), emptyList()))
        håndterSimuleringer(mapOf(
            Fagområde.SykepengerRefusjon to Pair(true, 1431)
        ))
        assertEquals(AVVENTER_SIMULERING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertFalse(person.personLogg.hasWarningsOrWorse())
    }

    @Test
    fun `simulering ved ingen refusjon`() {
        håndterYtelser(Inntektsmelding.Refusjon(INGEN, null, emptyList()))
        håndterSimuleringer(mapOf(
            Fagområde.Sykepenger to Pair(true, 1431)
        ))
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertFalse(person.personLogg.hasWarningsOrWorse())
    }

    private fun håndterYtelser(
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList())
    ) {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding(refusjon))
        person.håndter(ytelser())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
    }

    private fun ytelser(
        utbetalinger: List<Infotrygdperiode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Aktivitetslogg().let {
        val meldingsreferanseId = UUID.randomUUID()
        Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = "${1.vedtaksperiode.id(ORGNUMMER)}",
            infotrygdhistorikk = InfotrygdhistorikkElement.opprett(
                oppdatert = LocalDateTime.now(),
                hendelseId = meldingsreferanseId,
                perioder = utbetalinger,
                inntekter = emptyList(),
                arbeidskategorikoder = emptyMap(),
                ugyldigePerioder = emptyList(),
                harStatslønn = false
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepengeYtelse,
                svangerskapsytelse = svangerskapYtelse,
            ),
            pleiepenger = Pleiepenger(
                perioder = emptyList(),
            ),
            omsorgspenger = Omsorgspenger(
                perioder = emptyList(),
            ),
            opplæringspenger = Opplæringspenger(
                perioder = emptyList(),
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = emptyList(),
            ),
            dødsinfo = Dødsinfo(null),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList()),
            dagpenger = Dagpenger(emptyList()),
            aktivitetslogg = it
        ).apply {
            hendelse = this
        }
    }

    private fun sykmelding() =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018.toString(),
            aktørId = "aktørId",
            fødselsdato = UNG_PERSON_FØDSELSDATO,
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(Sykmeldingsperiode(førsteSykedag, sisteSykedag, 100.prosent)),
            sykmeldingSkrevet = førsteSykedag.atStartOfDay(),
            mottatt = sisteSykedag.atStartOfDay()
        ).apply {
            hendelse = this
        }

    private fun søknad() =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018.toString(),
            fødselsdato = UNG_PERSON_FØDSELSDATO,
            aktørId = "aktørId",
            orgnummer = ORGNUMMER,
            perioder = listOf(Sykdom(førsteSykedag, sisteSykedag, 100.prosent)),
            andreInntektskilder = emptyList(),
            sendtTilNAVEllerArbeidsgiver = sisteSykedag.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
        ).apply {
            hendelse = this
        }

    private fun inntektsmelding(
        refusjon: Inntektsmelding.Refusjon
    ) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = refusjon,
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            aktørId = "aktørId",
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(15))),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag() =
        Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode.id(ORGNUMMER)}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt 31000.månedlig
                }
            }),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt 31000.månedlig
                    }
                }, arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(
                    ORGNUMMER,
                    1.januar(2017)
                )
            )
        ).apply {
            hendelse = this
        }

    private fun håndterSimuleringer(simuleringsdetaljer: Map<Fagområde, Pair<Boolean, Int>> = mapOf(Fagområde.SykepengerRefusjon to Pair(true, 1431))) {
        hendelse.behov().filter { it.type == Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering }.forEach { simuleringsBehov ->
            val fagsystemId = simuleringsBehov.detaljer().getValue("fagsystemId") as String
            val fagområde = Fagområde.from(simuleringsBehov.detaljer().getValue("fagområde") as String)
            val utbetalingId = UUID.fromString(simuleringsBehov.kontekst().getValue("utbetalingId"))
            if (!simuleringsdetaljer.containsKey(fagområde)) return@forEach
            val (simuleringOk, dagsats) = simuleringsdetaljer.getValue(fagområde)
            person.håndter(simulering(simuleringOk, dagsats, fagområde, fagsystemId, utbetalingId))
        }
    }

    private fun simulering(
        simuleringOK: Boolean,
        dagsats: Int,
        fagområde: Fagområde,
        fagsystemId: String,
        utbetalingId: UUID) =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode.id(ORGNUMMER)}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            orgnummer = ORGNUMMER,
            fagsystemId = fagsystemId,
            fagområde = fagområde.verdi,
            simuleringOK = simuleringOK,
            melding = "",
            utbetalingId = utbetalingId,
            simuleringResultat = if (!simuleringOK) null else Simulering.SimuleringResultat(
                totalbeløp = 44361,
                perioder = listOf(
                    Simulering.SimulertPeriode(
                        periode = Periode(17.januar, 31.januar),
                        utbetalinger = listOf(
                            Simulering.SimulertUtbetaling(
                                forfallsdato = 1.februar,
                                utbetalesTil = Simulering.Mottaker(UNG_PERSON_FNR_2018.toString(), "Ung Person"),
                                feilkonto = false,
                                detaljer = listOf(
                                    Simulering.Detaljer(
                                        periode = Periode(17.januar, 31.januar),
                                        konto = "11111111111",
                                        beløp = dagsats * 11,
                                        klassekode = Simulering.Klassekode(
                                            "SPREFAG-IOP",
                                            "Sykepenger, Refusjon arbeidsgiver"
                                        ),
                                        uføregrad = 100,
                                        utbetalingstype = "YTELSE",
                                        tilbakeføring = false,
                                        sats = Simulering.Sats(dagsats.toDouble(), 11, "DAGLIG"),
                                        refunderesOrgnummer = ORGNUMMER
                                    )
                                )
                            )
                        )
                    ),
                    Simulering.SimulertPeriode(
                        periode = Periode(1.februar, 28.februar),
                        utbetalinger = listOf(
                            Simulering.SimulertUtbetaling(
                                forfallsdato = 1.mars,
                                utbetalesTil = Simulering.Mottaker(UNG_PERSON_FNR_2018.toString(), "Ung Person"),
                                feilkonto = false,
                                detaljer = listOf(
                                    Simulering.Detaljer(
                                        periode = Periode(1.februar, 28.februar),
                                        konto = "11111111111",
                                        beløp = dagsats * 20,
                                        klassekode = Simulering.Klassekode(
                                            "SPREFAG-IOP",
                                            "Sykepenger, Refusjon arbeidsgiver"
                                        ),
                                        uføregrad = 100,
                                        utbetalingstype = "YTELSE",
                                        tilbakeføring = false,
                                        sats = Simulering.Sats(dagsats.toDouble(), 20, "DAGLIG"),
                                        refunderesOrgnummer = ORGNUMMER
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ).apply {
            hendelse = this
        }
}
