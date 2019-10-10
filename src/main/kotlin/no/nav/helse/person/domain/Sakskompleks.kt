package no.nav.helse.person.domain

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelse.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.io.StringWriter
import java.util.*

class Sakskompleks internal constructor(
        private val id: UUID,
        private val aktørId: String
) {

    private var tilstand: Sakskomplekstilstand = StartTilstand

    private var sykdomstidslinje: Sykdomstidslinje? = null

    private val observers: MutableList<SakskompleksObserver> = mutableListOf()

    internal fun håndterNySøknad(søknad: NySykepengesøknad): Boolean {
        return passerMed(søknad).also {
            if (it) {
                tilstand.håndterNySøknad(this, søknad)
            }
        }
    }

    internal fun håndterSendtSøknad(søknad: SendtSykepengesøknad): Boolean {
        return passerMed(søknad).also {
            if (it) {
                tilstand.håndterSendtSøknad(this, søknad)
            }
        }
    }

    internal fun håndterInntektsmelding(inntektsmelding: Inntektsmelding) =
            passerMed(inntektsmelding).also {
                if (it) {
                    tilstand.håndterInntektsmelding(this, inntektsmelding)
                }
            }

    private fun passerMed(hendelse: Sykdomshendelse): Boolean {
        return true
    }

    private fun setTilstand(event: Event, nyTilstand: Sakskomplekstilstand, block: () -> Unit = {}) {
        tilstand.leaving()

        val previousStateName = tilstand.type
        val previousMemento = memento()

        tilstand = nyTilstand
        block()

        tilstand.entering()

        notifyObservers(tilstand.type, event, previousStateName, previousMemento)
    }

    enum class TilstandType {
        START,
        NY_SØKNAD_MOTTATT,
        SENDT_SØKNAD_MOTTATT,
        INNTEKTSMELDING_MOTTATT,
        KOMPLETT_SAK,
        TRENGER_MANUELL_HÅNDTERING

    }

    // Gang of four State pattern
    private interface Sakskomplekstilstand {

        val type: TilstandType

        fun håndterNySøknad(sakskompleks: Sakskompleks, søknad: NySykepengesøknad) {
            sakskompleks.setTilstand(søknad, TrengerManuellHåndteringTilstand)
        }

        fun håndterSendtSøknad(sakskompleks: Sakskompleks, søknad: SendtSykepengesøknad) {
            sakskompleks.setTilstand(søknad, TrengerManuellHåndteringTilstand)
        }

        fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmelding: Inntektsmelding) {
            sakskompleks.setTilstand(inntektsmelding, TrengerManuellHåndteringTilstand)
        }

        fun leaving() {
        }

        fun entering() {
        }

    }

    private fun slåSammenSykdomstidslinje(hendelse: Sykdomshendelse) {
        this.sykdomstidslinje = this.sykdomstidslinje?.plus(hendelse.sykdomstidslinje())
                ?: hendelse.sykdomstidslinje()
    }

    private object StartTilstand : Sakskomplekstilstand {

        override fun håndterNySøknad(sakskompleks: Sakskompleks, søknad: NySykepengesøknad) {
            sakskompleks.setTilstand(søknad, NySøknadMottattTilstand) {
                sakskompleks.sykdomstidslinje = søknad.sykdomstidslinje()
            }
        }

        override val type = Sakskompleks.TilstandType.START

    }

    private object NySøknadMottattTilstand : Sakskomplekstilstand {

        override fun håndterSendtSøknad(sakskompleks: Sakskompleks, søknad: SendtSykepengesøknad) {
            sakskompleks.setTilstand(søknad, SendtSøknadMottattTilstand) {
                sakskompleks.slåSammenSykdomstidslinje(søknad)
            }
        }

        override fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmelding: Inntektsmelding) {
            sakskompleks.setTilstand(inntektsmelding, InntektsmeldingMottattTilstand) {
                // TODO: blokkert fordi inntektsmelding ikke har tidslinje enda
                // sakskompleks.slåSammenSykdomstidslinje(inntektsmelding)
            }
        }

        override val type = TilstandType.NY_SØKNAD_MOTTATT

    }

    private object SendtSøknadMottattTilstand : Sakskomplekstilstand {

        override fun håndterInntektsmelding(sakskompleks: Sakskompleks, inntektsmelding: Inntektsmelding) {
            sakskompleks.setTilstand(inntektsmelding, KomplettSakTilstand) {
                // TODO: blokkert fordi inntektsmelding ikke har tidslinje enda
                // sakskompleks.slåSammenSykdomstidslinje(inntektsmelding)
            }
        }

        override val type = TilstandType.SENDT_SØKNAD_MOTTATT

    }

    private object InntektsmeldingMottattTilstand : Sakskomplekstilstand {

        override fun håndterSendtSøknad(sakskompleks: Sakskompleks, søknad: SendtSykepengesøknad) {
            sakskompleks.setTilstand(søknad, KomplettSakTilstand) {
                sakskompleks.slåSammenSykdomstidslinje(søknad)
            }
        }

        override val type = TilstandType.INNTEKTSMELDING_MOTTATT

    }

    private object KomplettSakTilstand : Sakskomplekstilstand {
        override val type = TilstandType.KOMPLETT_SAK

    }

    private object TrengerManuellHåndteringTilstand : Sakskomplekstilstand {
        override val type = TilstandType.TRENGER_MANUELL_HÅNDTERING

    }

    // Gang of four Memento pattern
    companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun restore(memento: Memento): Sakskompleks {
            val node = objectMapper.readTree(memento.state)

            val sakskompleks = Sakskompleks(
                    id = UUID.fromString(node["id"].textValue()),
                    aktørId = node["aktørId"].textValue()
            )

            sakskompleks.tilstand = when (TilstandType.valueOf(node["tilstand"].textValue())) {
                TilstandType.START -> StartTilstand
                TilstandType.NY_SØKNAD_MOTTATT -> NySøknadMottattTilstand
                TilstandType.SENDT_SØKNAD_MOTTATT -> SendtSøknadMottattTilstand
                TilstandType.INNTEKTSMELDING_MOTTATT -> InntektsmeldingMottattTilstand
                TilstandType.KOMPLETT_SAK -> KomplettSakTilstand
                TilstandType.TRENGER_MANUELL_HÅNDTERING -> TrengerManuellHåndteringTilstand
            }

            node["sykdomstidslinje"]?.let {
                sakskompleks.sykdomstidslinje = Sykdomstidslinje.fromJson(it.toString())
            }

            return sakskompleks
        }

    }

    internal fun memento(): Memento {
        val writer = StringWriter()
        val generator = JsonFactory().createGenerator(writer)

        generator.writeStartObject()
        generator.writeStringField("id", id.toString())
        generator.writeStringField("aktørId", aktørId)
        generator.writeStringField("tilstand", tilstand.type.name)

        sykdomstidslinje?.also {
            generator.writeFieldName("sykdomstidslinje")
            generator.writeRaw(":")
            generator.writeRaw(it.toJson())
        }

        generator.writeEndObject()

        generator.flush()

        return Memento(state = writer.toString())
    }

    fun jsonRepresentation(): SakskompleksJson{
        return SakskompleksJson(id = id, aktørId = aktørId, tilstandType = tilstand.type, sykdomstidslinje = objectMapper.readTree(sykdomstidslinje?.toJson()))
    }
    class Memento(internal val state: String) {
        override fun toString() = state

    }

    // Gang of four Observer pattern
    internal fun addObserver(observer: SakskompleksObserver) {
        observers.add(observer)
    }

    private fun notifyObservers(currentState: TilstandType, event: Event, previousState: TilstandType, previousMemento: Memento) {
        val event = SakskompleksObserver.StateChangeEvent(
                id = id,
                aktørId = aktørId,
                currentState = currentState,
                previousState = previousState,
                eventType = event.eventType(),
                currentMemento = memento(),
                previousMemento = previousMemento
        )

        observers.forEach { observer ->
            observer.sakskompleksChanged(event)
        }
    }
    data class SakskompleksJson(
            val id: UUID,
            val aktørId: String,
            val tilstandType: TilstandType,
            val sykdomstidslinje: JsonNode
    )
}
