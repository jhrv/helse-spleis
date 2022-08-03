package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
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
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.sisteBehov
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GodkjenningHendelseTest : AbstractPersonTest() {
    private companion object {
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 31.januar
    }

    private lateinit var hendelse: ArbeidstakerHendelse

    @Test
    fun `utbetaling er godkjent`() {
        håndterYtelser()
        person.håndter(simulering())
        person.håndter(utbetalingsgodkjenning(true))
        assertEquals(TIL_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `utbetaling ikke godkjent`() {
        håndterYtelser()
        person.håndter(simulering())
        person.håndter(utbetalingsgodkjenning(false))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `hendelse etter til utbetaling`() {
        håndterYtelser()
        person.håndter(simulering())
        person.håndter(utbetalingsgodkjenning(true))
        assertEquals(TIL_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
        person.håndter(ytelser())
        assertEquals(TIL_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
    }

    @Test
    fun `dobbelt svar fra saksbehandler`() {
        håndterYtelser()
        person.håndter(simulering())
        person.håndter(utbetalingsgodkjenning(true))
        person.håndter(utbetalingsgodkjenning(true))
        assertEquals(TIL_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `legger på periodetype på utgående godkjenningsbehov`() {
        håndterYtelser()
        person.håndter(simulering())
        assertEquals(1, hendelse.behov().size)
        assertEquals(Periodetype.FØRSTEGANGSBEHANDLING.name, hendelse.behov().first().detaljer()["periodetype"])
        person.håndter(utbetalingsgodkjenning(true))
    }

    private fun håndterYtelser() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(ytelser())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
    }

    private fun utbetalingsgodkjenning(godkjent: Boolean) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018.toString(),
        organisasjonsnummer = ORGNUMMER,
        utbetalingId = UUID.fromString(
            person.personLogg.sisteBehov(Behovtype.Godkjenning).kontekst()["utbetalingId"] ?: throw IllegalStateException(
                "Finner ikke utbetalingId i: ${
                    person.personLogg.sisteBehov(
                        Behovtype.Godkjenning
                    ).kontekst()
                }"
            )
        ),
        vedtaksperiodeId = "${1.vedtaksperiode.id(ORGNUMMER)}",
        saksbehandler = "Ola Nordmann",
        saksbehandlerEpost = "ola@nordmann.ss",
        utbetalingGodkjent = godkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = false,
    ).apply {
        hendelse = this
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
            infotrygdhistorikk = null,
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepengeYtelse,
                svangerskapsytelse = svangerskapYtelse
            ),
            pleiepenger = Pleiepenger(
                perioder = emptyList()
            ),
            omsorgspenger = Omsorgspenger(
                perioder = emptyList()
            ),
            opplæringspenger = Opplæringspenger(
                perioder = emptyList()
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = emptyList()
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

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            aktørId = "aktørId",
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
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
            inntektsvurdering = Inntektsvurdering(
                inntektperioderForSammenligningsgrunnlag {
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

    private fun simulering() =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode.id(ORGNUMMER)}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            orgnummer = ORGNUMMER,
            fagsystemId = person.personLogg.sisteBehov(Behovtype.Simulering).detaljer().getValue("fagsystemId") as String,
            fagområde = person.personLogg.sisteBehov(Behovtype.Simulering).detaljer().getValue("fagområde") as String,
            simuleringOK = true,
            melding = "",
            simuleringResultat = Simulering.SimuleringResultat(
                totalbeløp = 1000,
                perioder = emptyList()
            ),
            utbetalingId = UUID.fromString(person.personLogg.sisteBehov(Behovtype.Simulering).kontekst().getValue("utbetalingId"))
        ).apply {
            hendelse = this
        }
}
