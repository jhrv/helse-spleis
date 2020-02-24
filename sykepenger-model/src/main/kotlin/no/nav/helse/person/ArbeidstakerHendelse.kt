package no.nav.helse.person

import no.nav.helse.behov.BehovType
import no.nav.helse.hendelser.HendelseObserver
import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    internal val aktivitetslogger: Aktivitetslogger,
    internal val aktivitetslogg: Aktivitetslogg
) : IAktivitetslogger by aktivitetslogger, IAktivitetslogg by aktivitetslogg, Aktivitetskontekst {

    init {
        aktivitetslogg.kontekst(this)
    }

    private val hendelseObservers = mutableListOf<HendelseObserver>()

    fun addObserver(hendelseObserver: HendelseObserver) = hendelseObservers.add(hendelseObserver)

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String
    abstract fun organisasjonsnummer(): String

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return this.javaClass.canonicalName.split('.').last().let {
            SpesifikkKontekst(it, melding(it))
        }
    }

    internal open fun melding(klassName: String) = klassName

    internal fun need(kontekstId: UUID, behov: BehovType) {
        aktivitetslogg.need(melding = behov.navn)
        hendelseObservers.forEach { it.onBehov(kontekstId, behov) }
    }
}
