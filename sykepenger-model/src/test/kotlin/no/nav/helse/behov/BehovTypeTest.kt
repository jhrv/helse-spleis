package no.nav.helse.behov

import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.person.Vedtaksperiodekontekst
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class BehovTypeTest {
    private val vedtaksperiodekontekst = object : Vedtaksperiodekontekst {
        override val vedtaksperiodeId = UUID.randomUUID()
        override val organisasjonsnummer = "orgnummer"
        override val aktørId = "aktørId"
        override val fødselsnummer = "fnr"
        override val kontekstId = UUID.randomUUID()

        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            return SpesifikkKontekst("Vedtaksperiode", "Vedtaksperiode: ${vedtaksperiodeId}")
        }
    }

    @Test
    internal fun `konverterer et utbetalingsbehov til et map`() {
        val utbetaling = BehovType.Utbetaling(
            vedtaksperiodekontekst,
            "yes",
            listOf(Utbetalingslinje(1.januar, 2.januar, 100)),
            31.desember,
            "no"
        )

        val values = utbetaling.toMap()

        assertEquals(
            mapOf(
                "aktørId" to "aktørId",
                "fødselsnummer" to "fnr",
                "organisasjonsnummer" to "orgnummer",
                "vedtaksperiodeId" to vedtaksperiodekontekst.vedtaksperiodeId,
                "utbetalingsreferanse" to "yes",
                "utbetalingslinjer" to listOf(
                    mapOf(
                        "fom" to 1.januar,
                        "tom" to 2.januar,
                        "dagsats" to 100
                    )
                ),
                "maksdato" to 31.desember,
                "saksbehandler" to "no"
            ), values
        )
    }
}
