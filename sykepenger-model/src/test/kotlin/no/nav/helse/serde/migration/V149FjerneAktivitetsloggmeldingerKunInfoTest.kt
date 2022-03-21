package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V149FjerneAktivitetsloggmeldingerKunInfoTest : MigrationTest(V149FjerneAktivitetsloggmeldingerKunInfo()) {
    @Test
    fun `fjerner kun infomeldinger`() {
        assertMigration(
            "/migrations/149/expected.json",
            "/migrations/149/original.json"
        )
    }
}