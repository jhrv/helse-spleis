package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.serde.api.dto.Arbeidsgiverinntekt
import no.nav.helse.serde.api.dto.InntekterFraAOrdningen
import no.nav.helse.serde.api.dto.Inntektkilde
import no.nav.helse.serde.api.dto.OmregnetÅrsinntekt

// Samler opp hver arbeidsgivers siste generasjon av sammenligningsgrunnlag per skjæringstidspunkt
internal class OppsamletSammenligningsgrunnlagBuilder(person: Person) : PersonVisitor {
    private val akkumulator: MutableMap<String, NyesteInnslag> = mutableMapOf()
    internal fun orgnumre() = akkumulator.keys

    init {
        person.accept(this)
    }

    internal fun sammenligningsgrunnlag(organisasjonsnummer: String, skjæringstidspunkt: LocalDate) =
        akkumulator[organisasjonsnummer]?.sammenligningsgrunnlag(skjæringstidspunkt)

    override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        SammenligningsgrunnlagBuilder(arbeidsgiver).build()?.let { akkumulator[organisasjonsnummer] = it }
    }

    private class NyesteInnslag(
        private val sammenligningsgrunnlagDTO: Map<LocalDate, Double>
    ) {
        fun sammenligningsgrunnlag(skjæringstidspunkt: LocalDate) = sammenligningsgrunnlagDTO[skjæringstidspunkt]
    }

    private class SammenligningsgrunnlagBuilder(arbeidsgiver: Arbeidsgiver) : ArbeidsgiverVisitor {
        private var nyesteInnslag: NyesteInnslag? = null

        init {
            arbeidsgiver.accept(this)

        }

        fun build() = nyesteInnslag

        override fun preVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) {
            if (nyesteInnslag != null) return
            nyesteInnslag = NyesteInnslag(
                InntektsopplysningBuilder(innslag).build()
            )
        }
    }

    private class InntektsopplysningBuilder(innslag: Inntektshistorikk.Innslag) : InntekthistorikkVisitor {
        private val akkumulator = mutableMapOf<LocalDate, Double>()

        init {
            innslag.accept(this)
        }

        fun build() = akkumulator.toMap()

        override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
            skattComposite.rapportertInntekt(dato)?.rapportertInntekt()?.let {
                akkumulator.put(dato, InntektBuilder(it).build().årlig)
            }
        }
    }
}

internal data class IArbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetÅrsinntekt: IOmregnetÅrsinntekt?,
    val sammenligningsgrunnlag: Double? = null,
    val deaktivert: Boolean
) {
    internal fun toDTO(): Arbeidsgiverinntekt {
        return Arbeidsgiverinntekt(
            organisasjonsnummer = arbeidsgiver,
            omregnetÅrsinntekt = omregnetÅrsinntekt?.toDTO(),
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            deaktivert = deaktivert
        )
    }
}

internal data class IOmregnetÅrsinntekt(
    val kilde: IInntektkilde,
    val beløp: Double,
    val månedsbeløp: Double,
    val inntekterFraAOrdningen: List<IInntekterFraAOrdningen>? = null //kun gyldig for A-ordningen
) {
    internal fun toDTO(): OmregnetÅrsinntekt {
        return OmregnetÅrsinntekt(
            kilde = kilde.toDTO(),
            beløp = beløp,
            månedsbeløp = månedsbeløp,
            inntekterFraAOrdningen = inntekterFraAOrdningen?.map { it.toDTO() }
        )
    }
}

internal enum class IInntektkilde {
    Saksbehandler, Inntektsmelding, Infotrygd, AOrdningen, IkkeRapportert;

    internal fun toDTO() = when (this) {
        Saksbehandler -> Inntektkilde.Saksbehandler
        Inntektsmelding -> Inntektkilde.Inntektsmelding
        Infotrygd -> Inntektkilde.Infotrygd
        AOrdningen -> Inntektkilde.AOrdningen
        IkkeRapportert -> Inntektkilde.IkkeRapportert
    }
}

internal data class IInntekterFraAOrdningen(
    val måned: YearMonth,
    val sum: Double
) {
    internal fun toDTO(): InntekterFraAOrdningen {
        return InntekterFraAOrdningen(måned, sum)
    }
}



