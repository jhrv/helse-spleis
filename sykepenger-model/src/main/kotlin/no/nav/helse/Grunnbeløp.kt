package no.nav.helse

import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import java.time.LocalDate
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import kotlin.math.min

internal class Grunnbeløp private constructor(private val multiplier: Double) {
    private val grunnbeløp = listOf(
        106399.årlig.gyldigFra(1.mai(2021), gyldigSomMinsteinntektKrav = 24.mai(2021) ),
        101351.årlig.gyldigFra(1.mai(2020), virkningsdato = 21.september(2020), gyldigSomMinsteinntektKrav = 21.september(2020) ),
        99858.årlig.gyldigFra(1.mai(2019), gyldigSomMinsteinntektKrav = 27. mai(2019)),
        96883.årlig.gyldigFra(1.mai(2018)),
        93634.årlig.gyldigFra(1.mai(2017)),
        92576.årlig.gyldigFra(1.mai(2016)),
        90068.årlig.gyldigFra(1.mai(2015)),
        88370.årlig.gyldigFra(1.mai(2014)),
        85245.årlig.gyldigFra(1.mai(2013)),
        82122.årlig.gyldigFra(1.mai(2012)),
        79216.årlig.gyldigFra(1.mai(2011)),
        75641.årlig.gyldigFra(1.mai(2010)),
        72881.årlig.gyldigFra(1.mai(2009)),
        70256.årlig.gyldigFra(1.mai(2008)),
        66812.årlig.gyldigFra(1.mai(2007)),
        62892.årlig.gyldigFra(1.mai(2006)),
        60699.årlig.gyldigFra(1.mai(2005)),
        58778.årlig.gyldigFra(1.mai(2004)),
        56861.årlig.gyldigFra(1.mai(2003)),
        54170.årlig.gyldigFra(1.mai(2002)),
        51360.årlig.gyldigFra(1.mai(2001)),
        49090.årlig.gyldigFra(1.mai(2000)),
        46950.årlig.gyldigFra(1.mai(1999)),
        45370.årlig.gyldigFra(1.mai(1998)),
        42500.årlig.gyldigFra(1.mai(1997)),
        41000.årlig.gyldigFra(1.mai(1996)),
        39230.årlig.gyldigFra(1.mai(1995)),
        38080.årlig.gyldigFra(1.mai(1994)),
        37300.årlig.gyldigFra(1.mai(1993)),
        36500.årlig.gyldigFra(1.mai(1992)),
        35500.årlig.gyldigFra(1.mai(1991)),
        34100.årlig.gyldigFra(1.desember(1990)),
        34000.årlig.gyldigFra(1.mai(1990)),
        32700.årlig.gyldigFra(1.april(1989)),
        31000.årlig.gyldigFra(1.april(1988)),
        30400.årlig.gyldigFra(1.januar(1988)),
        29900.årlig.gyldigFra(1.mai(1987)),
        28000.årlig.gyldigFra(1.mai(1986)),
        26300.årlig.gyldigFra(1.januar(1986)),
        25900.årlig.gyldigFra(1.mai(1985)),
        24200.årlig.gyldigFra(1.mai(1984)),
        22600.årlig.gyldigFra(1.mai(1983)),
        21800.årlig.gyldigFra(1.januar(1983)),
        21200.årlig.gyldigFra(1.mai(1982)),
        19600.årlig.gyldigFra(1.oktober(1981)),
        19100.årlig.gyldigFra(1.mai(1981)),
        17400.årlig.gyldigFra(1.januar(1981)),
        16900.årlig.gyldigFra(1.mai(1980)),
        16100.årlig.gyldigFra(1.januar(1980)),
        15200.årlig.gyldigFra(1.januar(1979)),
        14700.årlig.gyldigFra(1.juli(1978)),
        14400.årlig.gyldigFra(1.desember(1977)),
        13400.årlig.gyldigFra(1.mai(1977)),
        13100.årlig.gyldigFra(1.januar(1977)),
        12100.årlig.gyldigFra(1.mai(1976)),
        11800.årlig.gyldigFra(1.januar(1976)),
        11000.årlig.gyldigFra(1.mai(1975)),
        10400.årlig.gyldigFra(1.januar(1975)),
        9700.årlig.gyldigFra(1.mai(1974)),
        9200.årlig.gyldigFra(1.januar(1974)),
        8500.årlig.gyldigFra(1.januar(1973)),
        7900.årlig.gyldigFra(1.januar(1972)),
        7500.årlig.gyldigFra(1.mai(1971)),
        7200.årlig.gyldigFra(1.januar(1971)),
        6800.årlig.gyldigFra(1.januar(1970)),
        6400.årlig.gyldigFra(1.januar(1969)),
        5900.årlig.gyldigFra(1.januar(1968)),
        5400.årlig.gyldigFra(1.januar(1967)),
    )

    internal fun beløp(dato: LocalDate) =
        gjeldende(dato).beløp(multiplier)

    internal fun beløp(dato: LocalDate, virkningFra: LocalDate) =
        gjeldende(dato, virkningFra).beløp(multiplier)

    internal fun dagsats(dato: LocalDate) = beløp(dato).rundTilDaglig()
    internal fun dagsats(dato: LocalDate, virkningFra: LocalDate) = beløp(dato, virkningFra).rundTilDaglig()

    private fun gjeldende(dato: LocalDate, virkningFra: LocalDate? = null) =
        HistoriskGrunnbeløp.gjeldendeGrunnbeløp(grunnbeløp, dato, virkningFra ?: dato)

    private fun oppfyllerMinsteInntekt(dato: LocalDate, inntekt: Inntekt) =
        inntekt >= minsteinntekt(dato)

    /*
     * Virkningstidspunktet for regulering av kravet til minsteinntekt for rett til ytelser etter folketrygdloven
     * kan settes til et tidspunkt etter virkningstidspunktet for for grunnbeløpet øvrig.
     * https://lovdata.no/forskrift/2021-05-21-1568/§6
    **/
    internal fun minsteinntekt(dato: LocalDate) = HistoriskGrunnbeløp.gjeldendeMinsteinntektGrunnbeløp(grunnbeløp, dato).beløp(multiplier)

    companion object {
        val `6G` = Grunnbeløp(6.0)
        val halvG = Grunnbeløp(0.5)
        val `2G` = Grunnbeløp(2.0)
        val `1G` = Grunnbeløp(1.0)

        private fun Inntekt.gyldigFra(gyldigFra: LocalDate, virkningsdato: LocalDate = gyldigFra, gyldigSomMinsteinntektKrav: LocalDate = gyldigFra) = HistoriskGrunnbeløp(this, gyldigFra, virkningsdato, gyldigSomMinsteinntektKrav)

        fun validerMinsteInntekt(skjæringstidspunkt: LocalDate, inntekt: Inntekt, alder: Alder, subsumsjonObserver: SubsumsjonObserver): Begrunnelse? {
            val gjeldendeGrense = if(alder.forhøyetInntektskrav(skjæringstidspunkt)) `2G` else halvG
            val oppfylt = gjeldendeGrense.oppfyllerMinsteInntekt(skjæringstidspunkt, inntekt)

            if (alder.forhøyetInntektskrav(skjæringstidspunkt)) {
                subsumsjonObserver.`§ 8-51 ledd 2`(oppfylt, skjæringstidspunkt, alder.alderPåDato(skjæringstidspunkt), inntekt, gjeldendeGrense.minsteinntekt(skjæringstidspunkt))
                if (oppfylt) return null
                return Begrunnelse.MinimumInntektOver67
            }
            subsumsjonObserver.`§ 8-3 ledd 2 punktum 1`(oppfylt, skjæringstidspunkt, inntekt, gjeldendeGrense.minsteinntekt(skjæringstidspunkt))
            if (oppfylt) return null
            return Begrunnelse.MinimumInntekt
        }
    }

    private class HistoriskGrunnbeløp( val beløp: Inntekt, val gyldigFra: LocalDate, val virkningsdato: LocalDate = gyldigFra, val gyldigMinsteinntektKrav: LocalDate) {
        init {
            require(virkningsdato >= gyldigFra) { "Virkningsdato må være nyere eller lik gyldighetstidspunktet" }
            require(gyldigMinsteinntektKrav >= gyldigFra) { "Virkningsdato for kravet til minsteinntekt må være nyere eller lik gyldighetstidspunktet" }
        }

        companion object {
            // TODO: Innføre startegy pattern
            fun gjeldendeGrunnbeløp(grunnbeløper: List<HistoriskGrunnbeløp>, dato: LocalDate, virkningFra: LocalDate): HistoriskGrunnbeløp {
                val virkningsdato = maxOf(dato, virkningFra)
                return grunnbeløper
                    .filter { virkningsdato >= it.virkningsdato && dato >= it.gyldigFra }
                    .maxByOrNull { it.virkningsdato }
                    ?: throw NoSuchElementException("Finner ingen grunnbeløp etter $dato")
            }

            fun gjeldendeMinsteinntektGrunnbeløp(grunnbeløper: List<HistoriskGrunnbeløp>, dato: LocalDate): HistoriskGrunnbeløp {
                return grunnbeløper
                    .filter { dato >= it.gyldigMinsteinntektKrav }
                    .maxByOrNull { it.gyldigMinsteinntektKrav }
                    ?: throw NoSuchElementException("Finner ingen grunnbeløp som gyldig som minsteinntektskrav for $dato")
            }
        }

        fun beløp(multiplier: Double) = beløp * multiplier
    }


    internal interface FastsattGrunnbeløpVisitor {
        fun visitGrunnbeløp(grunnbeløp: Inntekt, virkingstidspunkt: LocalDate, virkningstidspunktSomMinsteinntekt: LocalDate, faktor: Double, utregnet: Inntekt) {}
        fun previsitGrunnbeløp(
            grunnbeløp: Inntekt,
            virkingstidspunkt: LocalDate,
            virkningstidspunktSomMinsteinntekt: LocalDate,
            faktor: Double,
            utregnet: Inntekt,
        ) {}
        fun postvisitGrunnbeløp(
            grunnbeløp: Inntekt,
            virkingstidspunkt: LocalDate,
            virkningstidspunktSomMinsteinntekt: LocalDate,
            faktor: Double,
            utregnet: Inntekt,
        ) {}
    }

    internal class FastsattGrunnbeløp private constructor(private val grunnbeløp: HistoriskGrunnbeløp, private val multiplier: Double): Comparable<Inntekt>  {

        companion object {
            internal fun minsteinntekt(skjæringstidspunkt: LocalDate): FastsattGrunnbeløp {
                val historiskGrunnbeløp = HistoriskGrunnbeløp.
                gjeldendeMinsteinntektGrunnbeløp(Grunnbeløp(1.0).grunnbeløp, skjæringstidspunkt)
                return FastsattGrunnbeløp(historiskGrunnbeløp, 0.5)
            }
            internal fun minsteinntektForhøyet(skjæringstidspunkt: LocalDate): FastsattGrunnbeløp {
                val historiskGrunnbeløp = HistoriskGrunnbeløp.
                gjeldendeMinsteinntektGrunnbeløp(Grunnbeløp(1.0).grunnbeløp, skjæringstidspunkt)
                return FastsattGrunnbeløp(historiskGrunnbeløp, 2.0)
            }
            internal fun sykepengegrunnlagBregrensing(skjæringstidspunkt: LocalDate) {}

            internal fun deserialiser(
                grunnbeløp: Double,
                virkingstidspunkt: LocalDate,
                virkningstidspunktSomMinsteinntekt: LocalDate,
                faktor: Double,
            ) = FastsattGrunnbeløp(grunnbeløp.årlig.gyldigFra(virkingstidspunkt, virkningstidspunktSomMinsteinntekt), faktor)
        }

        override fun compareTo(other: Inntekt) =
            if (grunnbeløp.beløp(multiplier) == other) 0 else grunnbeløp.beløp(multiplier).compareTo(other)


        fun accept(visitor: FastsattGrunnbeløpVisitor) {
            visitor.previsitGrunnbeløp(
                grunnbeløp.beløp,
                grunnbeløp.gyldigFra,
                grunnbeløp.gyldigMinsteinntektKrav,
                multiplier,
                grunnbeløp.beløp(multiplier)
            )
            visitor.postvisitGrunnbeløp(
                grunnbeløp.beløp,
                grunnbeløp.gyldigFra,
                grunnbeløp.gyldigMinsteinntektKrav,
                multiplier,
                grunnbeløp.beløp(multiplier)
            )
        }
    }
}


// TODO: Slett etter refaktor er ferdig
internal class MinsteinntektVisitor(grunnbeløp: Grunnbeløp.FastsattGrunnbeløp) : Grunnbeløp.FastsattGrunnbeløpVisitor {
    private lateinit var minsteinntekt: Inntekt

    init {
        grunnbeløp.accept(this)
    }

    fun minsteinntekt(): Inntekt {
        return minsteinntekt
    }

    override fun postvisitGrunnbeløp(
        grunnbeløp: Inntekt,
        virkingstidspunkt: LocalDate,
        virkningstidspunktSomMinsteinntekt: LocalDate,
        faktor: Double,
        utregnet: Inntekt
    ) {
        minsteinntekt = utregnet
    }
}