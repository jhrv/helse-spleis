package no.nav.helse.spleis.db

import kotliquery.Query
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Personidentifikator
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.spleis.PostgresProbe
import javax.sql.DataSource

internal class PersonPostgresRepository(private val dataSource: DataSource) : PersonRepository {

    override fun hentPerson(personidentifikator: Personidentifikator) =
        hentPerson(queryOf("SELECT data FROM person WHERE fnr = ? ORDER BY id DESC LIMIT 1", personidentifikator.toLong()))

    private fun hentPerson(query: Query) =
        sessionOf(dataSource).use { session ->
            session.run(query.map {
                SerialisertPerson(it.string("data"))
            }.asSingle)
        }?.also {
            PostgresProbe.personLestFraDb()
        }
}
