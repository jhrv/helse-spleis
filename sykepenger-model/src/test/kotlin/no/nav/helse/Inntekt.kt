package no.nav.helse

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Sammenligningsgrunnlag
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.økonomi.Inntekt

internal val Inntekt.sykepengegrunnlag get() = sykepengegrunnlag(AbstractPersonTest.ORGNUMMER)

internal fun Inntekt.sykepengegrunnlag(orgnr: String) = sykepengegrunnlag(AbstractPersonTest.UNG_PERSON_FNR_2018.alder(), orgnr, 1.januar)
internal fun Inntekt.sykepengegrunnlag(alder: Alder) = sykepengegrunnlag(alder, AbstractPersonTest.ORGNUMMER, 1.januar)
internal fun Inntekt.sykepengegrunnlag(skjæringstidspunkt: LocalDate) =
    sykepengegrunnlag(AbstractPersonTest.UNG_PERSON_FNR_2018.alder(), AbstractPersonTest.ORGNUMMER, skjæringstidspunkt)

internal fun Inntekt.sykepengegrunnlag(alder: Alder, orgnr: String, skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver = SubsumsjonObserver.NullObserver) =
    Sykepengegrunnlag(
        alder = alder,
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning(
                orgnr,
                Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), this)
            )
        ),
        deaktiverteArbeidsforhold = emptyList(),
        skjæringstidspunkt = skjæringstidspunkt,
        sammenligningsgrunnlag = Sammenligningsgrunnlag(this, emptyList()),
        subsumsjonObserver = subsumsjonObserver
    )
internal fun Inntekt.sykepengegrunnlag(orgnr: String, skjæringstidspunkt: LocalDate, virkningstidspunkt: LocalDate) =
    Sykepengegrunnlag(
        alder = AbstractPersonTest.UNG_PERSON_FNR_2018.alder(),
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning(
                orgnr,
                Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), this)
            )
        ),
        deaktiverteArbeidsforhold = emptyList(),
        vurdertInfotrygd = false,
        sammenligningsgrunnlag = Sammenligningsgrunnlag(this, emptyList()),
        `6G` = Grunnbeløp.`6G`.beløp(skjæringstidspunkt, virkningstidspunkt)
    )