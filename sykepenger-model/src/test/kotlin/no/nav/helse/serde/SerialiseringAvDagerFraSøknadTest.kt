package no.nav.helse.serde

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Egenmelding
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utdanning
import no.nav.helse.januar
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SerialiseringAvDagerFraSøknadTest {

    @Test
    fun `perioder fra søknaden skal serialiseres og deserialiseres riktig - jackson`() {
        val person = person
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val personDeserialisert = SerialisertPerson(jsonBuilder.toString())
            .deserialize(MaskinellJurist())

        assertJsonEquals(person, personDeserialisert)
    }

    @Test
    fun `perioder fra søknaden skal serialiseres og deserialiseres riktig`() {
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val json = jsonBuilder.toString()

        val result = SerialisertPerson(json).deserialize(MaskinellJurist())
        val jsonBuilder2 = JsonBuilder()
        result.accept(jsonBuilder2)
        val json2 = jsonBuilder2.toString()

        assertEquals(json, json2)
        assertJsonEquals(person, result)
    }

    private val aktørId = "12345"
    private val fnr = "12029240045"
    private val orgnummer = "987654321"

    private lateinit var aktivitetslogg: Aktivitetslogg

    private lateinit var person: Person

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()

        person = sykmelding.person(MaskinellJurist()).apply {
            håndter(sykmelding)
            håndter(søknad)
        }
    }

    private val sykmelding get() = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fnr,
        aktørId = aktørId,
        fødselsdato = AbstractPersonTest.UNG_PERSON_FØDSELSDATO,
        orgnummer = orgnummer,
        sykeperioder = listOf(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent)),
        sykmeldingSkrevet = 4.april.atStartOfDay(),
        mottatt = 4.april.atStartOfDay()
    )

    private val søknad get() = Søknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fnr,
        fødselsdato = AbstractPersonTest.UNG_PERSON_FØDSELSDATO,
        aktørId = aktørId,
        orgnummer = orgnummer,
        perioder = listOf(
            Sykdom(1.januar,  2.januar, 100.prosent),
            Egenmelding(2.januar, 2.januar),
            Arbeid(3.januar, 3.januar),
            Ferie(4.januar, 4.januar),
            Permisjon(5.januar, 5.januar),
            Utdanning(5.januar, 5.januar)
        ),
        andreInntektskilder = emptyList(),
        sendtTilNAVEllerArbeidsgiver = 5.januar.atStartOfDay(),
        permittert = false,
        merknaderFraSykmelding = emptyList(),
        sykmeldingSkrevet = LocalDateTime.now()
    )
}

