package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.utbetalingstidslinje.Alder.Companion.alder

internal class FunksjonelleFeilTilVarsler(private val other: IAktivitetslogg) : IAktivitetslogg by other {
    override fun funksjonellFeil(melding: String) = varsel(melding)

    internal companion object {
        internal fun wrap(hendelse: PersonHendelse, block: () -> Unit) = hendelse.wrap(::FunksjonelleFeilTilVarsler, block)
    }
}

class Personopplysninger(
    private val personidentifikator: Personidentifikator,
    private val aktørId: String,
    private val fødselsdato: LocalDate
) {
    internal fun nyPerson(jurist: MaskinellJurist) = Person(
        personidentifikator = personidentifikator,
        aktørId = aktørId,
        alder = fødselsdato.alder,
        jurist = jurist
    )
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

    open fun personopplysninger(): Personopplysninger =
        throw IllegalStateException("${this::class.simpleName} inneholder ikke nødvendige personopplysninger.")

    fun person(jurist: MaskinellJurist) = personopplysninger().nyPerson(jurist)

    fun meldingsreferanseId() = meldingsreferanseId

    final override fun toSpesifikkKontekst() = this.javaClass.canonicalName.split('.').last().let {
        SpesifikkKontekst(it, mapOf(
            "meldingsreferanseId" to meldingsreferanseId().toString(),
            "aktørId" to aktørId(),
            "fødselsnummer" to fødselsnummer()
        ) + kontekst())
    }

    protected open fun kontekst(): Map<String, String> = emptyMap()

    fun toLogString() = aktivitetslogg.toString()

    override fun info(melding: String, vararg params: Any?) = aktivitetslogg.info(melding, *params)
    override fun varsel(melding: String) = aktivitetslogg.varsel(melding)
    override fun behov(type: Aktivitetslogg.Aktivitet.Behov.Behovtype, melding: String, detaljer: Map<String, Any?>) =
        aktivitetslogg.behov(type, melding, detaljer)
    override fun funksjonellFeil(melding: String) = aktivitetslogg.funksjonellFeil(melding)
    override fun logiskFeil(melding: String, vararg params: Any?) = aktivitetslogg.logiskFeil(melding, *params)
    override fun harAktiviteter() = aktivitetslogg.harAktiviteter()
    override fun harVarslerEllerVerre() = aktivitetslogg.harVarslerEllerVerre()
    override fun harFunksjonelleFeilEllerVerre() = aktivitetslogg.harFunksjonelleFeilEllerVerre()
    override fun aktivitetsteller() = aktivitetslogg.aktivitetsteller()
    override fun behov() = aktivitetslogg.behov()
    override fun barn() = aktivitetslogg.barn()
    override fun kontekst(kontekst: Aktivitetskontekst) = aktivitetslogg.kontekst(kontekst)
    override fun kontekst(person: Person) = aktivitetslogg.kontekst(person)
    override fun kontekster() = aktivitetslogg.kontekster()
    override fun hendelseskontekster() = aktivitetslogg.hendelseskontekster()
    override fun hendelseskontekst() = aktivitetslogg.hendelseskontekst()
    override fun toMap() = aktivitetslogg.toMap()

    override fun register(observer: AktivitetsloggObserver) {
        aktivitetslogg.register(observer)
    }
}
