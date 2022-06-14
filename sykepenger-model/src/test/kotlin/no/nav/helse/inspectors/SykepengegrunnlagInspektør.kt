package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.person.Sammenligningsgrunnlag
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import kotlin.properties.Delegates

internal val Sykepengegrunnlag.inspektør get() = SykepengegrunnlagInspektør(this)

internal class SykepengegrunnlagInspektør(sykepengegrunnlag: Sykepengegrunnlag) : VilkårsgrunnlagHistorikkVisitor {
    lateinit var minsteinntekt: Inntekt
    var oppfyllerMinsteinntektskrav: Boolean by Delegates.notNull<Boolean>()
    lateinit var sykepengegrunnlag: Inntekt
    lateinit var maksimalDagsats: Inntekt
    lateinit var beregningsgrunnlag: Inntekt
    var skjønnsmessigFastsattÅrsinntekt: Inntekt? = null
    lateinit var `6G`: Inntekt
    lateinit var deaktiverteArbeidsforhold: List<String>
    init {
        sykepengegrunnlag.accept(this)
    }

    override fun preVisitSykepengegrunnlag(
        sykepengegrunnlag1: Sykepengegrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Inntekt,
        skjønnsmessigFastsattÅrsinntekt: Inntekt?,
        beregningsgrunnlag: Inntekt,
        maksimalDagsats: Inntekt,
        `6G`: Inntekt,
        begrensning: Sykepengegrunnlag.Begrensning,
        deaktiverteArbeidsforhold: List<String>,
        vurdertInfotrygd: Boolean,
        minsteinntekt: Inntekt,
        oppfyllerMinsteinntektskrav: Boolean,
        sammenligningsgrunnlag: Sammenligningsgrunnlag?,
        avviksprosent: Prosent?
    ) {
        this.minsteinntekt = minsteinntekt
        this.oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav
        this.`6G` = `6G`
        this.sykepengegrunnlag = sykepengegrunnlag
        this.maksimalDagsats = maksimalDagsats
        this.skjønnsmessigFastsattÅrsinntekt = skjønnsmessigFastsattÅrsinntekt
        this.beregningsgrunnlag = beregningsgrunnlag
        this.deaktiverteArbeidsforhold = deaktiverteArbeidsforhold
    }
}