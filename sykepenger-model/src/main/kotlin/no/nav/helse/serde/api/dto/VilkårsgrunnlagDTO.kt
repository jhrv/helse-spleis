package no.nav.helse.serde.api.dto

import java.time.LocalDate
import java.time.YearMonth
import no.nav.helse.serde.api.speil.builders.SykepengegrunnlagsgrenseDTO

interface Vilkårsgrunnlag {
    val skjæringstidspunkt: LocalDate
    val omregnetÅrsinntekt: Double
    val sammenligningsgrunnlag: Double?
    val sykepengegrunnlag: Double
    val inntekter: List<Arbeidsgiverinntekt>
    val vilkårsgrunnlagtype: Vilkårsgrunnlagtype
}

enum class Vilkårsgrunnlagtype {
    INFOTRYGD,
    SPLEIS
}

data class SpleisVilkårsgrunnlag(
    override val skjæringstidspunkt: LocalDate,
    override val omregnetÅrsinntekt: Double,
    override val sammenligningsgrunnlag: Double,
    override val sykepengegrunnlag: Double,
    override val inntekter: List<Arbeidsgiverinntekt>,
    val avviksprosent: Double?,
    val grunnbeløp: Int,
    val sykepengegrunnlagsgrense: SykepengegrunnlagsgrenseDTO,
    val antallOpptjeningsdagerErMinst: Int,
    val opptjeningFra: LocalDate,
    val oppfyllerKravOmMinstelønn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val oppfyllerKravOmMedlemskap: Boolean?
) : Vilkårsgrunnlag {
    override val vilkårsgrunnlagtype = Vilkårsgrunnlagtype.SPLEIS
}

data class InfotrygdVilkårsgrunnlag(
    override val skjæringstidspunkt: LocalDate,
    override val omregnetÅrsinntekt: Double,
    override val sammenligningsgrunnlag: Double?,
    override val sykepengegrunnlag: Double,
    override val inntekter: List<Arbeidsgiverinntekt>
) : Vilkårsgrunnlag {
    override val vilkårsgrunnlagtype = Vilkårsgrunnlagtype.INFOTRYGD
}

data class Arbeidsgiverinntekt(
    val organisasjonsnummer: String,
    val omregnetÅrsinntekt: OmregnetÅrsinntekt?,
    val sammenligningsgrunnlag: Double? = null,
    val deaktivert: Boolean
)

enum class Inntektkilde {
    Saksbehandler, Inntektsmelding, Infotrygd, AOrdningen, IkkeRapportert
}

data class OmregnetÅrsinntekt(
    val kilde: Inntektkilde,
    val beløp: Double,
    val månedsbeløp: Double,
    val inntekterFraAOrdningen: List<InntekterFraAOrdningen>? = null //kun gyldig for A-ordningen
)

data class InntekterFraAOrdningen(
    val måned: YearMonth,
    val sum: Double
)
