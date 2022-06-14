package no.nav.helse.person

import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntektsopplysningPerArbeidsgiver
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.sammenligningsgrunnlag
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent

internal class Sammenligningsgrunnlag(
    internal val sammenligningsgrunnlag: Inntekt,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
) {

    internal constructor(arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>) : this(
        arbeidsgiverInntektsopplysninger.sammenligningsgrunnlag(),
        arbeidsgiverInntektsopplysninger
    )

    internal fun avviksprosent(sykepengegrunnlag: Sykepengegrunnlag, subsumsjonObserver: SubsumsjonObserver) =
        sykepengegrunnlag.avviksprosent(sammenligningsgrunnlag, subsumsjonObserver)

    internal fun avviksprosent(beregningsgrunnlag: Inntekt, subsumsjonObserver: SubsumsjonObserver = NullObserver) = beregningsgrunnlag.avviksprosent(sammenligningsgrunnlag).also { avviksprosent ->
        subsumsjonObserver.`§ 8-30 ledd 2 punktum 1`(Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT, beregningsgrunnlag, sammenligningsgrunnlag, avviksprosent)
    }

    internal fun accept(visitor: SykepengegrunnlagVisitor) {
        visitor.preVisitSammenligningsgrunnlag(this, sammenligningsgrunnlag)
        visitor.preVisitArbeidsgiverInntektsopplysninger()
        arbeidsgiverInntektsopplysninger.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgiverInntektsopplysninger()
        visitor.postVisitSammenligningsgrunnlag(this, sammenligningsgrunnlag)
    }

    internal fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> =
        arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()


}
