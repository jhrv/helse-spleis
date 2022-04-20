package no.nav.helse.spleis.db

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.e2e.SpleisDataSource.migratedDb
import no.nav.helse.spleis.e2e.resetDatabase
import no.nav.helse.spleis.meldinger.model.NySøknadMessage
import no.nav.helse.spleis.meldinger.model.OverstyrTidslinjeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private val fnr = "01011012345".somFødselsnummer()
// primært for å slutte å ha teite sql-feil
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HendelseRepositoryTest {
    private lateinit var dataSource: DataSource

    @BeforeAll
    internal fun setupAll() {
        dataSource = migratedDb
    }
    @BeforeEach
    internal fun setupEach() {
        resetDatabase()
    }

    @Test
    fun `skal klare å hente ny søknad-hendelse fra db`() {
        val repo = HendelseRepository(dataSource)
        val ingenEvents = repo.hentAlleHendelser(fnr)
        assertEquals(0, ingenEvents.size)
        repo.lagreMelding(TestMessages.nySøknad())
        val singleEvent = repo.hentAlleHendelser(fnr)
        assertEquals(1, singleEvent.size)
    }

    @Test
    fun `hente overstyring`() {
        val repo = HendelseRepository(dataSource)
        val ingenEvents = repo.hentAlleHendelser(fnr)
        assertEquals(0, ingenEvents.size)
        val overstyrTidslinjeMessage = TestMessages.overstyrTidslinje()
        repo.lagreMelding(overstyrTidslinjeMessage)
        val hentetFraDb = repo.finnOverstyring(fnr, overstyrTidslinjeMessage.id)
        assertEquals(overstyrTidslinjeMessage.id, UUID.fromString(hentetFraDb?.get("@id")?.asText()))
    }

    @Test
    fun `hente overstyring som ikke finnes`() {
        val repo = HendelseRepository(dataSource)
        val hentetFraDb = repo.finnOverstyring(fnr, UUID.randomUUID())
        assertNull(hentetFraDb)
    }
}

private object TestMessages {
    fun nySøknad() : NySøknadMessage {
        val now = LocalDateTime.now()
        val id = UUID.randomUUID()
        val jsonMessage = JsonMessage("""
        {
            "@id": "$id",
            "@event_name": "ny_soknad",
            "@opprettet": "$now",
            "fnr": "$fnr",
            "aktorId": "aktorId",
            "sykmeldingSkrevet": "$now",
            "fom": "2020-01-01",
            "tom": "2020-01-31",
            "arbeidsgiver": {
                "orgnummer": "orgnummer"
            },
            "soknadsperioder": []
        }
    """, MessageProblems("")).also { packet ->
            packet.requireKey("@id")
            packet.requireKey("@event_name")
            packet.requireKey("@opprettet")
            packet.requireKey("sykmeldingSkrevet")
            packet.requireKey("fom")
            packet.requireKey("tom")
            packet.requireKey("fnr")
            packet.requireKey("aktorId")
            packet.requireKey("arbeidsgiver")
            packet.requireKey("arbeidsgiver.orgnummer")
            packet.requireKey("soknadsperioder")
        }
        return NySøknadMessage(jsonMessage)
    }

    fun overstyrTidslinje(): OverstyrTidslinjeMessage {
        val now = LocalDateTime.now()
        val id = UUID.randomUUID()
        val json = """
        {
            "@id": "$id",
            "@event_name": "overstyr_tidslinje",
            "@opprettet": "$now",
            "fødselsnummer": "$fnr",
            "aktørId": "aktorId",
            "organisasjonsnummer": "981312311",
            "dager": [{
                "type": "Feriedag",
                "dato": "2018-01-01"
            }]
        }
        """
        val jsonMessage = JsonMessage(json, MessageProblems("")).also { packet ->
            packet.requireKey("@id")
            packet.requireKey("@event_name")
            packet.requireKey("@opprettet")
            packet.requireKey("fødselsnummer")
            packet.requireKey("organisasjonsnummer")
            packet.requireKey("aktørId")
            packet.requireKey("dager", "dager.type", "dager.dato")
            packet.interestedIn("dager.grad", "@replay")
        }
        return OverstyrTidslinjeMessage(jsonMessage)
    }
}