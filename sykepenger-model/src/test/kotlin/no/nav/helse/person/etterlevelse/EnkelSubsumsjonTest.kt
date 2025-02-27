package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import no.nav.helse.person.etterlevelse.MaskinellJurist.*
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.VILKAR_OPPFYLT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class EnkelSubsumsjonTest {

    private lateinit var vurderinger: List<Subsumsjon>

    @BeforeEach
    fun beforeEach() {
        vurderinger = emptyList()
    }

    @Test
    fun `enkel vurdering`() {
        nyVurdering()
        assertEquals(1, vurderinger.size)
    }

    @Test
    fun `enkel vurdering som gjøres flere ganger innenfor samme hendelse forekommer kun en gang`() {
        nyVurdering()
        nyVurdering()

        assertEquals(1, vurderinger.size)
    }

    @Test
    fun `vurderinger med ulike data utgjør hvert sitt innslag`() {
        nyVurdering(VILKAR_OPPFYLT)
        nyVurdering(VILKAR_IKKE_OPPFYLT)

        assertEquals(2, vurderinger.size)
    }

    private fun nyVurdering(
        utfall: Utfall = VILKAR_OPPFYLT,
        versjon: LocalDate = LocalDate.MAX,
        paragraf: Paragraf = Paragraf.PARAGRAF_8_2,
        ledd: Ledd = 1.ledd,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any> = emptyMap(),
        output: Map<String, Any> = emptyMap(),
        kontekster: Map<String, KontekstType> = emptyMap()
    ) {
        vurderinger = EnkelSubsumsjon(utfall, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster).sammenstill(vurderinger)
    }
}
