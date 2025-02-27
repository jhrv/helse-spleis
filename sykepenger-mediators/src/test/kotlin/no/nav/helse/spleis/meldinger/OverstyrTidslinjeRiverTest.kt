package no.nav.helse.spleis.meldinger

import no.nav.helse.desember
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.TestMessageFactory
import org.junit.jupiter.api.Test

internal class OverstyrTidslinjeRiverTest : RiverTest() {
    private val fabrikk = TestMessageFactory("fnr", "aktør", "orgnr", 1000.0, 24.desember(2000))

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        OverstyrTidlinjeRiver(rapidsConnection, mediator)
    }

    @Test
    fun `overstyring uten dager`() {
        assertErrors(fabrikk.lagOverstyringTidslinje(emptyList()))
    }
}
