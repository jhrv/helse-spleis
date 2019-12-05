package no.nav.helse.hendelser.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.hendelser.SykdomshendelseType
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.UtenforOmfangException
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.egenmeldingsdag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.util.*

class InntektsmeldingHendelse private constructor(hendelseId: String, private val inntektsmelding: Inntektsmelding) :
    ArbeidstakerHendelse, SykdomstidslinjeHendelse(hendelseId) {
    constructor(inntektsmelding: Inntektsmelding) : this(UUID.randomUUID().toString(), inntektsmelding)

    companion object {

        fun fromJson(jsonNode: JsonNode): InntektsmeldingHendelse {
            return InntektsmeldingHendelse(
                jsonNode["hendelseId"].textValue(),
                Inntektsmelding(jsonNode["inntektsmelding"])
            )
        }
    }

    fun beregnetInntekt() = inntektsmelding.beregnetInntekt
        ?: throw IllegalStateException("Vi kan ikke håndtere inntektsmeldinger uten beregnet inntekt")

    fun refusjon() =
        inntektsmelding.refusjon

    fun endringIRefusjoner() =
        inntektsmelding.endringIRefusjoner

    override fun fødselsnummer() = inntektsmelding.arbeidstakerFnr

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Inntektsmelding

    override fun aktørId() =
        inntektsmelding.arbeidstakerAktorId

    override fun rapportertdato() =
        inntektsmelding.mottattDato

    override fun organisasjonsnummer() =
        inntektsmelding.virksomhetsnummer!!

    override fun kanBehandles(): Boolean {
        return inntektsmelding.kanBehandles()
    }

    override fun sykdomstidslinje(): Sykdomstidslinje {
        val arbeidsgiverperiode = inntektsmelding.arbeidsgiverperioder
            .takeIf { it.isNotEmpty() }
            ?.map { Sykdomstidslinje.egenmeldingsdager(it.fom, it.tom, this) }
            ?.reduce { acc, sykdomstidslinje ->
                if (acc.overlapperMed(sykdomstidslinje)) {
                    throw UtenforOmfangException(
                        "Inntektsmeldingen inneholder overlappende arbeidsgiverperioder",
                        this
                    )
                }
                acc.plus(sykdomstidslinje, Sykdomstidslinje.Companion::ikkeSykedag)
            }?.let {
                Sykdomstidslinje.ikkeSykedager(
                    it.førsteDag().minusDays(16),
                    it.førsteDag().minusDays(1),
                    this
                ) + it
            }

        val ferietidslinje = inntektsmelding.ferie
            .map { Sykdomstidslinje.ferie(it.fom, it.tom, this) }
            .takeUnless { it.isEmpty() }
            ?.reduce { resultat, sykdomstidslinje -> resultat + sykdomstidslinje }

        return arbeidsgiverperiode.plus(ferietidslinje) ?: egenmeldingsdag(inntektsmelding.førsteFraværsdag, this)
    }

    private fun Sykdomstidslinje?.plus(other: Sykdomstidslinje?): Sykdomstidslinje? {
        if (other == null) return this
        return this?.plus(other) ?: other
    }

    override fun toJson(): JsonNode {
        return (super.toJson() as ObjectNode).apply {
            put("type", SykdomshendelseType.InntektsmeldingMottatt.name)
            set<ObjectNode>("inntektsmelding", inntektsmelding.jsonNode)
        }
    }
}

