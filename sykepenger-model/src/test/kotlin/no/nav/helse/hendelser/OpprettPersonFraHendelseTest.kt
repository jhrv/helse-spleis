package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.dsl.Hendelsefabrikk
import no.nav.helse.januar
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somFødselsnummer
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

class OpprettPersonFraHendelseTest {

    private val fabrikk = Hendelsefabrikk("aktørid", "12121212345".somFødselsnummer(), LocalDate.of(1992, 1, 1), "orgnum")

    @Test
    fun `sykmelding skal kunne opprette en person`() {
        val person = fabrikk.lagSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent)).person(MaskinellJurist())
        assertNotNull(person)
    }

    @Test
    fun `søknad skal kunne opprette en person`() {
        val person = fabrikk.lagSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent)).person(MaskinellJurist())
        assertNotNull(person)
    }

    @Test
    fun `inntektsmelding skal kunne opprette en person`() {
        val person = fabrikk.lagInntektsmelding(listOf(1.januar til 16.januar), 1000.månedlig).person(MaskinellJurist())
        assertNotNull(person)
    }
}