package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somFødselsnummer
import no.nav.helse.utbetalingstidslinje.Alder

internal class ErrorsTilWarnings(private val other: IAktivitetslogg) : IAktivitetslogg by other {
    override fun error(melding: String, vararg params: Any?) = warn(melding, params)

    internal companion object {
        internal fun wrap(hendelse: PersonHendelse, block: () -> Unit) = hendelse.wrap(::ErrorsTilWarnings, block)
    }
}

abstract class PersonHendelse protected constructor(
    private val meldingsreferanseId: UUID,
    protected val fødselsnummer: String,
    protected val aktørId: String,
    private var aktivitetslogg: IAktivitetslogg
) : IAktivitetslogg, Aktivitetskontekst {

    init {
        aktivitetslogg.kontekst(this)
    }

    internal fun wrap(other: (IAktivitetslogg) -> IAktivitetslogg, block: () -> Unit): IAktivitetslogg {
        val kopi = aktivitetslogg
        aktivitetslogg = other(aktivitetslogg)
        block()
        aktivitetslogg = kopi
        return this
    }

    fun aktørId() = aktørId
    fun fødselsnummer() = fødselsnummer
    fun person(jurist: MaskinellJurist) = Person(
        aktørId = aktørId,
        fødselsnummer = fødselsnummer.somFødselsnummer(),
        alder = Alder(fødselsdato()),
        jurist = jurist
    )

    internal fun meldingsreferanseId() = meldingsreferanseId

    final override fun toSpesifikkKontekst() = this.javaClass.canonicalName.split('.').last().let {
        SpesifikkKontekst(it, mapOf(
            "meldingsreferanseId" to meldingsreferanseId().toString(),
            "aktørId" to aktørId(),
            "fødselsnummer" to fødselsnummer()
        ) + kontekst())
    }

    protected open fun kontekst(): Map<String, String> = emptyMap()
    protected open fun fødselsdato(): LocalDate = throw RuntimeException("Har ikke fødselsdato på hendelsen ${this::class.java.simpleName}")
    fun toLogString() = aktivitetslogg.toString()

    override fun info(melding: String, vararg params: Any?) = aktivitetslogg.info(melding, *params)
    override fun warn(melding: String, vararg params: Any?) = aktivitetslogg.warn(melding, *params)
    override fun behov(type: Aktivitetslogg.Aktivitet.Behov.Behovtype, melding: String, detaljer: Map<String, Any?>) =
        aktivitetslogg.behov(type, melding, detaljer)
    override fun error(melding: String, vararg params: Any?) = aktivitetslogg.error(melding, *params)
    override fun severe(melding: String, vararg params: Any?) = aktivitetslogg.severe(melding, *params)
    override fun hasActivities() = aktivitetslogg.hasActivities()
    override fun hasWarningsOrWorse() = aktivitetslogg.hasWarningsOrWorse()
    override fun hasErrorsOrWorse() = aktivitetslogg.hasErrorsOrWorse()
    override fun aktivitetsteller() = aktivitetslogg.aktivitetsteller()
    override fun behov() = aktivitetslogg.behov()
    override fun barn() = aktivitetslogg.barn()
    override fun kontekst(kontekst: Aktivitetskontekst) = aktivitetslogg.kontekst(kontekst)
    override fun kontekst(person: Person) = aktivitetslogg.kontekst(person)
    override fun kontekster() = aktivitetslogg.kontekster()
    override fun hendelseskontekster() = aktivitetslogg.hendelseskontekster()
    override fun hendelseskontekst() = aktivitetslogg.hendelseskontekst()
    override fun toMap() = aktivitetslogg.toMap()
}
