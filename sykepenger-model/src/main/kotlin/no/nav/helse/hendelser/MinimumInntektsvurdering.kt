package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.Grunnbeløp
import no.nav.helse.MinsteinntektVisitor
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.økonomi.Inntekt

internal fun validerMinimumInntekt(
    aktivitetslogg: IAktivitetslogg,
    minsteinntekt: Grunnbeløp.FastsattGrunnbeløp,
    alder: Alder,
    skjæringstidspunkt: LocalDate,
    grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
    subsumsjonObserver: SubsumsjonObserver
): Boolean {

    val oppfylt = grunnlagForSykepengegrunnlag.oppfyllerKravTilMinimumInntekt(minsteinntekt)
    val grunnlag = grunnlagForSykepengegrunnlag.grunnlagForSykepengegrunnlag
    val alderPåSkjæringstidspunkt = alder.alderPåDato(skjæringstidspunkt)
    val minimumInntekt = MinsteinntektVisitor(minsteinntekt).minsteinntekt()

    if (alder.forhøyetInntektskrav(skjæringstidspunkt))
        subsumsjonObserver.`§ 8-51 ledd 2`(oppfylt, skjæringstidspunkt, alderPåSkjæringstidspunkt, grunnlag, minimumInntekt)
    else
        subsumsjonObserver.`§ 8-3 ledd 2 punktum 1`(oppfylt, skjæringstidspunkt, grunnlag, minimumInntekt)

    if (oppfylt) aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")
    else aktivitetslogg.warn("Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag")

    return oppfylt
}