package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.contains
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V149FjerneAktivitetsloggmeldingerKunInfo : JsonMigration(version = 149) {
    private companion object {
        private val log = LoggerFactory.getLogger(V149FjerneAktivitetsloggmeldingerKunInfo::class.java)
        private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }
    override val description: String = "Fjerner aktiviteter hvor alle meldingene på hendelsen bare er INFO-meldinger (Påminnelse og Utbetalingshistorikk)"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val hendelser = jsonNode.path("aktivitetslogg").path("kontekster")
            .mapIndexed { indeks, kontekst ->
                indeks to kontekst
            }
            .filter { (_, kontekst) ->
                kontekst.path("kontekstMap").contains("meldingsreferanseId")
            }.associate { (indeks, kontekst) ->
                indeks to Hendelse(
                    indeks = indeks,
                    type = kontekst.path("kontekstType").asText(),
                    meldingsreferanseId = kontekst.path("kontekstMap").path("meldingsreferanseId").asText()
                )
            }

        val aktiviteter = jsonNode.path("aktivitetslogg").path("aktiviteter") as ArrayNode
        aktiviteter.forEach { aktivitet ->
            val hendelse = aktivitet.path("kontekster")
                .map(JsonNode::asInt)
                .firstNotNullOfOrNull { indeks -> hendelser[indeks] }

            hendelse?.leggTilAktivitet(Aktivitet(
                alvorlighetsgrad = aktivitet.path("alvorlighetsgrad").asText(),
                melding = aktivitet.path("melding").asText(),
                tidsstempel = LocalDateTime.parse(aktivitet.path("tidsstempel").asText(), tidsstempelformat),
                node = aktivitet
            ))
        }

        hendelser
            .filterValues { it.bareInfo() }
            .also { log.info("Sletter aktivitetslogg-innslag fra ${it.size} hendelser som utelukkende består av INFO-meldinger") }
            .forEach { (_, hendelse) ->
                hendelse.slettAktiviteter()
            }

        (jsonNode.path("aktivitetslogg") as ObjectNode).replace("aktiviteter", ArrayNode(serdeObjectMapper.nodeFactory, aktiviteter.filterNot {
            it.path("slett").asBoolean(false)
        }))
    }

    private class Hendelse(
        val indeks: Int,
        val type: String,
        val meldingsreferanseId: String
    ) {
        private val aktiviteter = mutableListOf<Aktivitet>()

        fun bareInfo() = type in setOf("Påminnelse", "Utbetalingshistorikk") && aktiviteter.all { it.alvorlighetsgrad == "INFO" }

        fun slettAktiviteter() {
            aktiviteter.forEach { aktivitet ->
                aktivitet.slett()
            }
        }

        fun leggTilAktivitet(aktivitet: Aktivitet) {
            this.aktiviteter.add(aktivitet)
        }
    }

    private class Aktivitet(
        val alvorlighetsgrad: String,
        val melding: String,
        val tidsstempel: LocalDateTime,
        val node: JsonNode
    ) {
        fun slett() {
            (node as ObjectNode).put("slett", true)
        }
    }
}
