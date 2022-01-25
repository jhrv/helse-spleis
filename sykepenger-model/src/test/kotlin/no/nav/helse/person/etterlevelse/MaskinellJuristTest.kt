package no.nav.helse.person.etterlevelse

import no.nav.helse.person.*
import no.nav.helse.somFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class MaskinellJuristTest {

    @Test
    fun `jurist lytter på endringer av kontekst`() {
        val vedtaksperiodeJurist = MaskinellJurist()
            .medFødselsnummer("10052088033".somFødselsnummer())
            .medOrganisasjonsnummer("123456789")
            .medVedtaksperiode(UUID.fromString("6bce6c83-28ab-4a8c-b7f6-8402988bc8fc"), emptyList())

        vedtaksperiodeJurist.`§8-2 ledd 1`(true, LocalDate.now(), 1, emptyList(), 1)

        assertKontekster(
            vedtaksperiodeJurist.vurderinger()[0],
            "fødselsnummer" to "10052088033",
            "organisasjonsnummer" to "123456789",
            "vedtaksperiodeId" to "6bce6c83-28ab-4a8c-b7f6-8402988bc8fc"
        )
    }

    @Test
    fun `alltid nyeste kontekst som gjelder`() {
        val arbeidsgiverJurist = MaskinellJurist()
            .medFødselsnummer("10052088033".somFødselsnummer())
            .medOrganisasjonsnummer("123456789")
            .medOrganisasjonsnummer("987654321")

        arbeidsgiverJurist.`§8-2 ledd 1`(true, LocalDate.now(), 1, emptyList(), 1)

        assertKontekster(
            arbeidsgiverJurist.vurderinger()[0],
            "fødselsnummer" to "10052088033",
            "organisasjonsnummer" to "987654321",
        )
    }

    private fun assertKontekster(juridiskVurdering: JuridiskVurdering, vararg kontekster: Pair<String, String>) {
        val inspektør = object : JuridiskVurderingVisitor {
            lateinit var kontekster: Map<String, String>

            init {
                juridiskVurdering.accept(this)
            }

            override fun preVisitVurdering(
                oppfylt: Boolean,
                versjon: LocalDate,
                paragraf: Paragraf,
                ledd: Ledd,
                punktum: List<Punktum>,
                bokstaver: List<Bokstav>,
                input: Map<String, Any>,
                output: Map<String, Any>,
                kontekster: Map<String, String>
            ) {
                this.kontekster = kontekster
            }
        }

        assertEquals(
            kontekster.toList().sortedBy { it.first },
            inspektør.kontekster.toList().sortedBy { it.first }
        )
    }
}
