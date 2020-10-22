package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.sykdomstidslinje.*
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.*

class Inntektsmelding(
    meldingsreferanseId: UUID,
    private val refusjon: Refusjon,
    private val orgnummer: String,
    private val fødselsnummer: String,
    private val aktørId: String,
    internal val førsteFraværsdag: LocalDate?,
    internal val beregnetInntekt: Inntekt,
    private val arbeidsgiverperioder: List<Periode>,
    ferieperioder: List<Periode>,
    private val arbeidsforholdId: String?,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private var beingQualified = false

    private val beste = { venstre: Dag, høyre: Dag ->
        when {
            venstre::class == høyre::class -> venstre
            venstre is UkjentDag -> høyre
            høyre is UkjentDag -> venstre
            venstre is Arbeidsgiverdag || venstre is ArbeidsgiverHelgedag -> venstre
            høyre is Arbeidsgiverdag || høyre is ArbeidsgiverHelgedag -> høyre
            venstre is Sykedag -> venstre
            høyre is Sykedag -> høyre
            venstre is Feriedag && høyre is Arbeidsdag -> venstre
            høyre is Feriedag && venstre is Arbeidsdag -> høyre
            venstre is Feriedag && høyre is FriskHelgedag -> venstre
            høyre is Feriedag && venstre is FriskHelgedag -> høyre
            else -> høyre.problem(venstre)
        }
    }

    private var sykdomstidslinje: Sykdomstidslinje = (
        arbeidsgivertidslinje(arbeidsgiverperioder, førsteFraværsdag)
            + ferietidslinje(ferieperioder)
            + nyFørsteFraværsdagtidslinje(førsteFraværsdag)
        ).merge(beste)

    init {
        if (arbeidsgiverperioder.isEmpty() && førsteFraværsdag == null) severe("Arbeidsgiverperiode er tom og førsteFraværsdag er null")
    }

    private fun arbeidsgivertidslinje(
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate?
    ): List<Sykdomstidslinje> {
        val arbeidsgiverdager = arbeidsgiverperioder.map { it.asArbeidsgivertidslinje() }.merge(beste)

        var arbeidsdager = Sykdomstidslinje.arbeidsdager(arbeidsgiverdager.periode(), kilde)

        if (førsteFraværsdag?.let {
                arbeidsgiverdager.periode()?.endInclusive?.plusDays(1)?.erHelgedagRettFør(it)
            } == true) {
            arbeidsdager +=
                Sykdomstidslinje.arbeidsdager(
                    arbeidsgiverdager.sisteDag().plusDays(1),
                    førsteFraværsdag.minusDays(1),
                    kilde
                )
        }

        return listOfNotNull(arbeidsgiverdager, arbeidsdager)
    }

    private fun ferietidslinje(ferieperioder: List<Periode>): List<Sykdomstidslinje> =
        ferieperioder.map { it.asFerietidslinje() }

    private fun nyFørsteFraværsdagtidslinje(førsteFraværsdag: LocalDate?): List<Sykdomstidslinje> =
        listOf(førsteFraværsdag?.let { Sykdomstidslinje.arbeidsgiverdager(it, it, 100, kilde) } ?: Sykdomstidslinje())

    private fun Periode.asArbeidsgivertidslinje() = Sykdomstidslinje.arbeidsgiverdager(start, endInclusive, 100, kilde)
    private fun Periode.asFerietidslinje() = Sykdomstidslinje.feriedager(start, endInclusive, kilde)

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun periode() =
        super.periode().let {
            Periode(
                listOfNotNull(sykdomstidslinje.førsteSykedagEtter(it.start), it.start).max()!!,
                it.endInclusive
            )
        }

    // Pad days prior to employer-paid days with assumed work days
    override fun padLeft(dato: LocalDate) {
        if (arbeidsgiverperioder.isEmpty()) return  // No justification to pad
        if (dato >= sykdomstidslinje.førsteDag()) return  // No need to pad if sykdomstidslinje early enough
        sykdomstidslinje += Sykdomstidslinje.arbeidsdager(
            dato,
            sykdomstidslinje.førsteDag().minusDays(1),
            this.kilde
        )
    }

    override fun valider(periode: Periode): Aktivitetslogg {
        refusjon.valider(aktivitetslogg, periode, beregnetInntekt)
        if (arbeidsgiverperioder.isEmpty()) aktivitetslogg.warn("Inntektsmelding inneholder ikke arbeidsgiverperiode. Kontroller at det kun er én arbeidsgiver. Flere arbeidsforhold støttes ikke av systemet")
        if (arbeidsforholdId != null && arbeidsforholdId.isNotBlank()) aktivitetslogg.warn("ArbeidsforholdsID er fylt ut i inntektsmeldingen. Kontroller om brukeren har flere arbeidsforhold i samme virksomhet. Flere arbeidsforhold støttes ikke av systemet foreløpig.")
        begrunnelseForReduksjonEllerIkkeUtbetalt?.takeIf(String::isNotBlank)?.also {
            aktivitetslogg.warn(
                "Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: %s. Vurder om dette har betydning for rett til sykepenger og beregning av arbeidsgiverperiode",
                it
            )
        }
        return aktivitetslogg
    }

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = orgnummer

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.håndter(this)

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk) {
        val beregningsdato = sykdomstidslinje.beregningsdato() ?: førsteFraværsdag ?: return

        if (beregningsdato != førsteFraværsdag) {
            warn("Første fraværsdag oppgitt i inntektsmeldingen er ulik den systemet har beregnet. Utbetal kun hvis dagsatsen er korrekt")
        }

        inntektshistorikk.add(
            beregningsdato.minusDays(1),  // Assuming salary is the day before the first sykedag
            meldingsreferanseId(),
            beregnetInntekt,
            Inntektshistorikk.Inntektsendring.Kilde.INNTEKTSMELDING
        )
    }

    internal fun addInntekt(inntektshistorikk: InntektshistorikkVol2) {
        val beregningsdato = sykdomstidslinje.beregningsdato() ?: førsteFraværsdag ?: return

        if (beregningsdato != førsteFraværsdag) {
            warn("Første fraværsdag oppgitt i inntektsmeldingen er ulik den systemet har beregnet. Utbetal kun hvis dagsatsen er korrekt")
        }

        inntektshistorikk {
            addInntektsmelding(
                beregningsdato,
                meldingsreferanseId(),
                beregnetInntekt
            )
        }
    }

    internal fun beingQualified() {
        beingQualified = true
    }

    internal fun isNotQualified() = !beingQualified

    class Refusjon(
        private val opphørsdato: LocalDate?,
        private val inntekt: Inntekt?,
        private val endringerIRefusjon: List<LocalDate> = emptyList()
    ) {

        internal fun valider(
            aktivitetslogg: Aktivitetslogg,
            periode: Periode,
            beregnetInntekt: Inntekt
        ): Aktivitetslogg {
            when {
                inntekt == null -> aktivitetslogg.error("Arbeidsgiver forskutterer ikke (krever ikke refusjon)")
                inntekt != beregnetInntekt -> aktivitetslogg.error("Inntektsmelding inneholder beregnet inntekt og refusjon som avviker med hverandre")
                opphørerRefusjon(periode) -> aktivitetslogg.error("Arbeidsgiver opphører refusjon i perioden")
                opphørsdato != null -> aktivitetslogg.error("Arbeidsgiver opphører refusjon")
                endrerRefusjon(periode) -> aktivitetslogg.error("Arbeidsgiver endrer refusjon i perioden")
                endringerIRefusjon.isNotEmpty() -> aktivitetslogg.error("Arbeidsgiver har endringer i refusjon")
            }
            return aktivitetslogg
        }

        private fun opphørerRefusjon(periode: Periode) =
            opphørsdato?.let { it in periode } ?: false

        private fun endrerRefusjon(periode: Periode) =
            endringerIRefusjon.any { it in periode }
    }
}
