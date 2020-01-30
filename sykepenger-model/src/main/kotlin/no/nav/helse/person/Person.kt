package no.nav.helse.person

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import java.util.*

private const val CURRENT_SKJEMA_VERSJON = 3

class Person private constructor(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val arbeidsgivere: MutableList<Arbeidsgiver>,
    private val hendelser: MutableList<ArbeidstakerHendelse>,
    private val aktivitetslogger: Aktivitetslogger
) : VedtaksperiodeObserver {
    private val skjemaVersjon = CURRENT_SKJEMA_VERSJON

    constructor(
        aktørId: String,
        fødselsnummer: String
    ) : this(aktørId, fødselsnummer, mutableListOf(), mutableListOf(), Aktivitetslogger())

    private val observers = mutableListOf<PersonObserver>()

    fun håndter(nySøknad: ModelNySøknad) {
        registrer(nySøknad, "Behandler ny søknad")
        Validation(nySøknad).also {
            it.onError { invaliderAllePerioder(nySøknad) }
            it.valider { ValiderSykdomshendelse(nySøknad) }
            val arbeidsgiver = finnEllerOpprettArbeidsgiver(nySøknad)
            it.valider { ValiderKunEnArbeidsgiver(arbeidsgivere) }
            it.valider { ArbeidsgiverHåndterHendelse(nySøknad, arbeidsgiver) }
        }
        nySøknad.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(sendtSøknad: ModelSendtSøknad) {
        registrer(sendtSøknad, "Behandler sendt søknad")
        Validation(sendtSøknad).also {
            it.onError { invaliderAllePerioder(sendtSøknad) }
            it.valider { ValiderSykdomshendelse(sendtSøknad) }
            val arbeidsgiver = finnEllerOpprettArbeidsgiver(sendtSøknad)
            it.valider { ValiderKunEnArbeidsgiver(arbeidsgivere) }
            it.valider { ArbeidsgiverHåndterHendelse(sendtSøknad, arbeidsgiver) }
        }
        sendtSøknad.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(inntektsmelding: ModelInntektsmelding) {
        registrer(inntektsmelding, "Behandler inntektsmelding")
        Validation(inntektsmelding).also {
            it.onError { invaliderAllePerioder(inntektsmelding) }
            it.valider { ValiderSykdomshendelse(inntektsmelding) }
            val arbeidsgiver = finnEllerOpprettArbeidsgiver(inntektsmelding)
            it.valider { ValiderKunEnArbeidsgiver(arbeidsgivere) }
            it.valider { ArbeidsgiverHåndterHendelse(inntektsmelding, arbeidsgiver) }
        }
        inntektsmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(ytelser: ModelYtelser) {
        registrer(ytelser, "Behandler historiske utbetalinger og inntekter")
        finnArbeidsgiver(ytelser)?.håndter(this, ytelser)
        ytelser.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(manuellSaksbehandling: ModelManuellSaksbehandling) {
        registrer(manuellSaksbehandling, "Behandler manuell saksbehandling")
        finnArbeidsgiver(manuellSaksbehandling)?.håndter(manuellSaksbehandling)
        manuellSaksbehandling.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
        registrer(vilkårsgrunnlag, "Behandler vilkårsgrunnlag")
        finnArbeidsgiver(vilkårsgrunnlag)?.håndter(vilkårsgrunnlag)
        vilkårsgrunnlag.kopierAktiviteterTil(aktivitetslogger)
    }

    fun håndter(påminnelse: ModelPåminnelse) {
        påminnelse.info("Behandler påminnelse")
        if (true == finnArbeidsgiver(påminnelse)?.håndter(påminnelse)) return
        påminnelse.warn("Fant ikke arbeidsgiver eller vedtaksperiode")
        observers.forEach {
            it.vedtaksperiodeIkkeFunnet(
                PersonObserver.VedtaksperiodeIkkeFunnetEvent(
                    vedtaksperiodeId = UUID.fromString(påminnelse.vedtaksperiodeId()),
                    aktørId = påminnelse.aktørId(),
                    fødselsnummer = påminnelse.fødselsnummer(),
                    organisasjonsnummer = påminnelse.organisasjonsnummer()
                )
            )
        }
        påminnelse.kopierAktiviteterTil(aktivitetslogger)
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
        observers.forEach {
            it.personEndret(
                PersonObserver.PersonEndretEvent(
                    aktørId = aktørId,
                    sykdomshendelse = event.sykdomshendelse,
                    memento = memento(),
                    fødselsnummer = fødselsnummer
                )
            )
        }
    }

    fun addObserver(observer: PersonObserver) {
        observers.add(observer)
        arbeidsgivere.forEach { it.addObserver(observer) }
    }

    internal fun accept(visitor: PersonVisitor) {
        visitor.preVisitPerson(this, aktørId, fødselsnummer)
        visitor.preVisitHendelser()
        hendelser.forEach { it.accept(visitor) }
        visitor.postVisitHendelser()
        visitor.visitPersonAktivitetslogger(aktivitetslogger)
        visitor.preVisitArbeidsgivere()
        arbeidsgivere.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgivere()
        visitor.postVisitPerson(this, aktørId, fødselsnummer)
    }

    private fun registrer(hendelse: ArbeidstakerHendelse, melding: String) {
        hendelser.add(hendelse)
        hendelse.info(melding)
    }

    private fun invaliderAllePerioder(arbeidstakerHendelse: ArbeidstakerHendelse) {
        aktivitetslogger.info("Invaliderer alle perioder for alle arbeidsgivere")
        arbeidsgivere.forEach { arbeidsgiver ->
            arbeidsgiver.invaliderPerioder(arbeidstakerHendelse)
        }
    }

    private fun finnEllerOpprettArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.finnEllerOpprett(orgnr) {
                hendelse.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", orgnr)
                arbeidsgiver(orgnr)
            }
        }

    private fun finnArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.finn(orgnr).also {
                if (it == null) hendelse.error("Finner ikke arbeidsgiver")
            }
        }

    private fun MutableList<Arbeidsgiver>.finn(orgnr: String) = find { it.organisasjonsnummer() == orgnr }

    private fun MutableList<Arbeidsgiver>.finnEllerOpprett(orgnr: String, creator: () -> Arbeidsgiver) =
        finn(orgnr) ?: run {
                val newValue = creator()
                add(newValue)
                newValue
            }

    private fun arbeidsgiver(organisasjonsnummer: String) =
        Arbeidsgiver(organisasjonsnummer).also {
            it.addObserver(this)
            observers.forEach { observer ->
                it.addObserver(observer)
            }
        }

    fun memento() =
        Memento(
            aktørId = this.aktørId,
            fødselsnummer = this.fødselsnummer,
            skjemaVersjon = this.skjemaVersjon,
            arbeidsgivere = this.arbeidsgivere.map { it.memento() }
        )

    class Memento internal constructor(
        internal val aktørId: String,
        internal val fødselsnummer: String,
        internal val skjemaVersjon: Int,
        internal val arbeidsgivere: List<Arbeidsgiver.Memento>
    ) {

        companion object {
            private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

            fun fromString(state: String): Memento {
                val jsonNode = objectMapper.readTree(state)

                if (!jsonNode.hasNonNull("skjemaVersjon")) throw PersonskjemaForGammelt(-1, CURRENT_SKJEMA_VERSJON)

                val skjemaVersjon = jsonNode["skjemaVersjon"].intValue()

                if (skjemaVersjon < CURRENT_SKJEMA_VERSJON) throw PersonskjemaForGammelt(
                    skjemaVersjon,
                    CURRENT_SKJEMA_VERSJON
                )

                return Memento(
                    aktørId = jsonNode["aktørId"].textValue(),
                    fødselsnummer = jsonNode["fødselsnummer"].textValue(),
                    skjemaVersjon = skjemaVersjon,
                    arbeidsgivere = jsonNode["arbeidsgivere"].map {
                        Arbeidsgiver.Memento.fromString(it.toString())
                    }
                )
            }
        }

        fun state(): String =
            objectMapper.convertValue<ObjectNode>(
                mapOf(
                    "aktørId" to this.aktørId,
                    "fødselsnummer" to this.fødselsnummer,
                    "skjemaVersjon" to this.skjemaVersjon
                )
            ).also {
                this.arbeidsgivere.fold(it.putArray("arbeidsgivere")) { result, current ->
                    result.addRawValue(RawValue(current.state()))
                }
            }.toString()
    }

    companion object {
        fun restore(memento: Memento): Person {
            return Person(memento.aktørId, memento.fødselsnummer)
                .apply {
                    this.arbeidsgivere.addAll(memento.arbeidsgivere.map { arbeidsgiverMemento ->
                        Arbeidsgiver.restore(arbeidsgiverMemento).also { arbeidsgiver ->
                            arbeidsgiver.addObserver(this)
                        }
                    })
                }
        }
    }
}
