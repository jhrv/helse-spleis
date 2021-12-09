package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

internal typealias Melding = KClass<out SykdomstidslinjeHendelse>

abstract class SykdomstidslinjeHendelse(
    meldingsreferanseId: UUID,
    private val opprettet: LocalDateTime,
    melding: Melding? = null,
    private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(meldingsreferanseId, aktivitetslogg) {

    protected constructor(meldingsreferanseId: UUID, other: SykdomstidslinjeHendelse) : this(meldingsreferanseId, other.opprettet, null, other.aktivitetslogg)

    private var nesteFom: LocalDate? = null

    internal val kilde: Hendelseskilde = Hendelseskilde(melding ?: this::class, meldingsreferanseId(), opprettet)

    internal class Hendelseskilde(
        private val type: String,
        private val meldingsreferanseId: UUID,
        private val tidsstempel: LocalDateTime
    ) {
        internal constructor(
            hendelse: Melding,
            meldingsreferanseId: UUID,
            tidsstempel: LocalDateTime
        ) : this(kildenavn(hendelse), meldingsreferanseId, tidsstempel)

        companion object {
            internal val INGEN = Hendelseskilde(SykdomstidslinjeHendelse::class, UUID.randomUUID(), LocalDateTime.now())

            private fun kildenavn(hendelse: Melding): String =
                hendelse.simpleName ?: "Ukjent"
        }

        override fun toString() = type
        internal fun tidsstempel() = tidsstempel
        internal fun meldingsreferanseId() = meldingsreferanseId
        internal fun erAvType(meldingstype: Melding) = this.type == kildenavn(meldingstype)
        internal fun toJson() = mapOf("type" to toString(), "id" to meldingsreferanseId(), "tidsstempel" to tidsstempel())
    }

    internal abstract fun sykdomstidslinje(): Sykdomstidslinje

    internal fun erRelevant(other: Periode) = !trimmetForbi() && erRelevantMed(other)

    private fun erRelevantMed(other: Periode) = overlappsperiode()?.overlapperMed(other) ?: false

    protected open fun overlappsperiode(): Periode? = sykdomstidslinje().periode()

    internal fun trimLeft(dato: LocalDate) {
        nesteFom = dato.plusDays(1)
    }

    private val aldri = LocalDate.MIN til LocalDate.MIN
    private fun trimmetForbi() = periode() == aldri

    internal fun periode(): Periode {
        val periode = overlappsperiode() ?: aldri
        val fom = nesteFom ?: return periode
        if (fom > periode.endInclusive) return aldri
        return (sykdomstidslinje().førsteSykedagEtter(fom) ?: fom) til periode.endInclusive
    }

    internal open fun validerIkkeOppgittFlereArbeidsforholdMedSykmelding(): IAktivitetslogg = this

    internal abstract fun valider(periode: Periode): IAktivitetslogg

    internal abstract fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver)

    internal open fun padLeft(dato: LocalDate) {}

    override fun equals(other: Any?): Boolean = other is SykdomstidslinjeHendelse
        && this.meldingsreferanseId() == other.meldingsreferanseId()

    override fun hashCode(): Int {
        return meldingsreferanseId().hashCode()
    }
}

