package no.nav.helse.serde.reflection

import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.readResource
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.serde.serialize
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

internal class AktivitetsloggMapTest {

    @Test
    fun `lagring av kontekst indices`() {
        val jurist = MaskinellJurist()
        val originalJson = "/aktivitetslogg/kontekst_indices.json".readResource()
        val person = SerialisertPerson(originalJson).deserialize(jurist)
        val json = person.serialize()
        JSONAssert.assertEquals(originalJson, json.json, JSONCompareMode.STRICT)
    }
}