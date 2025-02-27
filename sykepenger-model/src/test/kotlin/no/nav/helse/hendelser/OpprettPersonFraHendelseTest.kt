package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somPersonidentifikator
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class OpprettPersonFraHendelseTest {

    private val fabrikk = ArbeidsgiverHendelsefabrikk(
        aktørId = "aktørid",
        personidentifikator = "01019212345".somPersonidentifikator(),
        organisasjonsnummer = "orgnum",
        fødselsdato = 24.desember(2000)
    )

    @Test
    fun `sykmelding skal kunne opprette ny person`() {
        fabrikk.lagSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent)).person(MaskinellJurist())
    }

    @Test
    fun `søknad skal kunne opprette ny person`() {
        fabrikk.lagSøknad(Sykdom(1.januar, 31.januar, 100.prosent)).person(MaskinellJurist())
    }

    @Test
    fun `inntektsmelding skal kunne opprette ny person`() {
        fabrikk.lagInntektsmelding(listOf(1.januar til 16.januar), 1000.månedlig).person(MaskinellJurist())
    }

    @Test
    fun `andre hendelser skal ikke kunne opprette ny person`() {
        assertThrows<IllegalStateException> {
            fabrikk.lagPåminnelse(UUID.randomUUID(), AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, LocalDateTime.now()).person(
                MaskinellJurist()
            )
        }
    }
}