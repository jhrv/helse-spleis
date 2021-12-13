package no.nav.helse.person


import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-28 ledd 3 bokstav a`
import no.nav.helse.person.Inntektshistorikk.Innslag.Companion.nyesteId
import no.nav.helse.serde.reflection.Inntektsopplysningskilde
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class Inntektshistorikk {

    private val historikk = mutableListOf<Innslag>()

    private val innslag
        get() = (historikk.firstOrNull()?.clone() ?: Innslag(UUID.randomUUID()))
            .also { historikk.add(0, it) }

    internal companion object {
        internal val NULLUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    internal fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitInntekthistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitInntekthistorikk(this)
    }

    internal fun nyesteId() = historikk.nyesteId()

    internal fun isNotEmpty() = historikk.isNotEmpty()

    internal fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, dato: LocalDate, førsteFraværsdag: LocalDate?): Inntektsopplysning? =
        grunnlagForSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag) ?: skjæringstidspunkt
            .takeIf { it <= dato }
            ?.let { historikk.firstOrNull()?.grunnlagForSykepengegrunnlagFraInfotrygd(it til dato) }

    internal fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): Inntektsopplysning? =
        historikk.firstOrNull()?.grunnlagForSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag)

    internal fun grunnlagForSammenligningsgrunnlag(dato: LocalDate): Inntektsopplysning? =
        historikk.firstOrNull()?.grunnlagForSammenligningsgrunnlag(dato)

    internal fun sykepengegrunnlagKommerFraSkatt(skjæringstidspunkt: LocalDate) =
        grunnlagForSykepengegrunnlag(skjæringstidspunkt, skjæringstidspunkt).let { it == null || it is SkattComposite }

    private fun harGrunnlagForSykepengegrunnlag(dato: LocalDate, førsteFraværsdag: LocalDate?) =
        this.grunnlagForSykepengegrunnlag(dato, førsteFraværsdag) != null

    private fun harGrunnlagForSammenligningsgrunnlag(dato: LocalDate) = grunnlagForSammenligningsgrunnlag(dato) != null

    internal fun harGrunnlagForSykepengegrunnlagEllerSammenligningsgrunnlag(dato: LocalDate, førsteFraværsdag: LocalDate?) =
        harGrunnlagForSykepengegrunnlag(dato, førsteFraværsdag) || harGrunnlagForSammenligningsgrunnlag(dato)

    internal class Innslag(private val id: UUID) {
        private val inntekter = mutableListOf<Inntektsopplysning>()

        internal fun accept(visitor: InntekthistorikkVisitor) {
            visitor.preVisitInnslag(this, id)
            inntekter.forEach { it.accept(visitor) }
            visitor.postVisitInnslag(this, id)
        }

        internal fun clone() = Innslag(UUID.randomUUID()).also {
            it.inntekter.addAll(this.inntekter)
        }

        internal fun add(inntektsopplysning: Inntektsopplysning) {
            if (inntekter.all { it.kanLagres(inntektsopplysning) }) {
                inntekter.removeIf { it.skalErstattesAv(inntektsopplysning) }
                inntekter.add(inntektsopplysning)
            }
        }

        internal fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
            inntekter
                .sorted()
                .mapNotNull { it.grunnlagForSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag) }
                .firstOrNull()

        internal fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
            inntekter
                .sorted()
                .mapNotNull { it.grunnlagForSammenligningsgrunnlag(dato) }
                .firstOrNull()

        internal fun grunnlagForSykepengegrunnlagFraInfotrygd(periode: Periode) =
            inntekter
                .filterIsInstance<Infotrygd>()
                .sorted()
                .mapNotNull { it.grunnlagForSykepengegrunnlag(periode) }
                .firstOrNull()

        internal companion object {
            internal fun List<Innslag>.nyesteId() = this.first().id
        }
    }

    internal interface Inntektsopplysning : Comparable<Inntektsopplysning> {
        val dato: LocalDate
        val prioritet: Int
        fun accept(visitor: InntekthistorikkVisitor)
        fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): Inntektsopplysning? = null
        fun grunnlagForSykepengegrunnlag(): Inntekt
        fun grunnlagForSammenligningsgrunnlag(dato: LocalDate): Inntektsopplysning? = null
        fun grunnlagForSammenligningsgrunnlag(): Inntekt
        fun skalErstattesAv(other: Inntektsopplysning): Boolean
        override fun compareTo(other: Inntektsopplysning) =
            (-this.dato.compareTo(other.dato)).takeUnless { it == 0 } ?: -this.prioritet.compareTo(other.prioritet)

        fun kanLagres(other: Inntektsopplysning) = true
    }

    internal class Saksbehandler(
        private val id: UUID,
        override val dato: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        override val prioritet = 100

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitSaksbehandler(this, dato, hendelseId, beløp, tidsstempel)
        }

        override fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) = takeIf { it.dato == skjæringstidspunkt }
        override fun grunnlagForSykepengegrunnlag(): Inntekt = beløp

        override fun grunnlagForSammenligningsgrunnlag(): Inntekt = error("Saksbehandler har ikke grunnlag for sammenligningsgrunnlag")

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Saksbehandler && this.dato == other.dato

        internal fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "dato" to dato,
            "hendelseId" to hendelseId,
            "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
            "kilde" to Inntektsopplysningskilde.SAKSBEHANDLER,
            "tidsstempel" to tidsstempel
        )
    }

    internal class Infotrygd(
        private val id: UUID,
        override val dato: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        override val prioritet = 80

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInfotrygd(this, dato, hendelseId, beløp, tidsstempel)
        }

        // TODO: egen test for å bruke førstefraværsdag her: https://trello.com/c/QFYSoFOs
        override fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) = takeIf { it.dato == skjæringstidspunkt }
        override fun grunnlagForSykepengegrunnlag(): Inntekt = beløp

        internal fun grunnlagForSykepengegrunnlag(periode: Periode) = takeIf { it.dato in periode }

        override fun grunnlagForSammenligningsgrunnlag(): Inntekt = error("Infotrygd har ikke grunnlag for sammenligningsgrunnlag")

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Infotrygd && this.dato == other.dato

        internal fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "dato" to dato,
            "hendelseId" to hendelseId,
            "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
            "kilde" to Inntektsopplysningskilde.INFOTRYGD,
            "tidsstempel" to tidsstempel
        )
    }

    internal class Inntektsmelding(
        private val id: UUID,
        override val dato: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        override val prioritet = 60

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInntektsmelding(this, dato, hendelseId, beløp, tidsstempel)
        }

        override fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
            takeIf { førsteFraværsdag != null && YearMonth.from(skjæringstidspunkt) == YearMonth.from(førsteFraværsdag) && it.dato == førsteFraværsdag }

        override fun grunnlagForSykepengegrunnlag(): Inntekt = beløp

        override fun grunnlagForSammenligningsgrunnlag(): Inntekt = error("Inntektsmelding har ikke grunnlag for sammenligningsgrunnlag")

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Inntektsmelding && this.dato == other.dato

        override fun kanLagres(other: Inntektsopplysning) =
            other !is Inntektsmelding || this.dato != other.dato

        internal fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "dato" to dato,
            "hendelseId" to hendelseId,
            "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
            "kilde" to Inntektsopplysningskilde.INNTEKTSMELDING,
            "tidsstempel" to tidsstempel
        )
    }

    internal class SkattComposite(
        private val id: UUID,
        private val inntektsopplysninger: List<Skatt>
    ) : Inntektsopplysning {

        override val dato = inntektsopplysninger.first().dato
        override val prioritet = inntektsopplysninger.first().prioritet

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.preVisitSkatt(this, id, dato)
            inntektsopplysninger.forEach { it.accept(visitor) }
            visitor.postVisitSkatt(this, id, dato)
        }

        override fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
            takeIf { inntektsopplysninger.any { it.grunnlagForSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag) != null } }

        override fun grunnlagForSykepengegrunnlag(): Inntekt {
            val (inntekterSisteTreMåneder, alleAndre) = inntektsopplysninger.partition { it.erRelevant(3) }
            return inntekterSisteTreMåneder
                .map(Skatt::grunnlagForSykepengegrunnlag)
                .summer()
                .div(3)
                .also {
                    //TODO: fix aktivitetslogg
                    Aktivitetslogg().`§8-28 ledd 3 bokstav a`(true, alleAndre, inntekterSisteTreMåneder, it)
                }
        }

        override fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
            takeIf { inntektsopplysninger.any { it.grunnlagForSammenligningsgrunnlag(dato) != null } }

        override fun grunnlagForSammenligningsgrunnlag(): Inntekt =
            inntektsopplysninger
                .filter { it.erRelevant(12) }
                .map(Skatt::grunnlagForSammenligningsgrunnlag)
                .summer()
                .div(12)

        internal fun sammenligningsgrunnlag() = grunnlagForSammenligningsgrunnlag(dato)?.grunnlagForSammenligningsgrunnlag()

        override fun skalErstattesAv(other: Inntektsopplysning): Boolean =
            this.inntektsopplysninger.any { it.skalErstattesAv(other) }
                || (other is SkattComposite && other.inntektsopplysninger.any { this.skalErstattesAv(it) })
    }

    internal sealed class Skatt(
        override val dato: LocalDate,
        protected val hendelseId: UUID,
        protected val beløp: Inntekt,
        protected val måned: YearMonth,
        protected val type: Inntekttype,
        protected val fordel: String,
        protected val beskrivelse: String,
        protected val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        internal enum class Inntekttype {
            LØNNSINNTEKT,
            NÆRINGSINNTEKT,
            PENSJON_ELLER_TRYGD,
            YTELSE_FRA_OFFENTLIGE
        }

        internal fun erRelevant(måneder: Long) = måned.isWithinRangeOf(dato, måneder)

        internal class Sykepengegrunnlag(
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime = LocalDateTime.now()
        ) : Skatt(
            dato,
            hendelseId,
            beløp,
            måned,
            type,
            fordel,
            beskrivelse,
            tidsstempel
        ) {
            override val prioritet = 40

            override fun accept(visitor: InntekthistorikkVisitor) {
                visitor.visitSkattSykepengegrunnlag(
                    this,
                    dato,
                    hendelseId,
                    beløp,
                    måned,
                    type,
                    fordel,
                    beskrivelse,
                    tidsstempel
                )
            }

            override fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
                takeIf { this.dato == skjæringstidspunkt && måned.isWithinRangeOf(skjæringstidspunkt, 3) }

            override fun grunnlagForSykepengegrunnlag(): Inntekt = beløp

            override fun grunnlagForSammenligningsgrunnlag(): Inntekt = error("Sykepengegrunnlag har ikke grunnlag for sammenligningsgrunnlag")

            override fun skalErstattesAv(other: Inntektsopplysning) =
                other is Sykepengegrunnlag && this.dato == other.dato && this.tidsstempel != other.tidsstempel
        }

        internal class Sammenligningsgrunnlag(
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime = LocalDateTime.now()
        ) :
            Skatt(dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel) {
            override val prioritet = 20

            override fun accept(visitor: InntekthistorikkVisitor) {
                visitor.visitSkattSammenligningsgrunnlag(
                    this,
                    dato,
                    hendelseId,
                    beløp,
                    måned,
                    type,
                    fordel,
                    beskrivelse,
                    tidsstempel
                )
            }

            override fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
                takeIf { this.dato == dato && måned.isWithinRangeOf(dato, 12) }

            override fun grunnlagForSammenligningsgrunnlag(): Inntekt = beløp

            override fun grunnlagForSykepengegrunnlag(): Inntekt = error("Sammenligningsgrunnlag har ikke grunnlag for sykepengegrunnlag")

            override fun skalErstattesAv(other: Inntektsopplysning) =
                other is Sammenligningsgrunnlag && this.dato == other.dato
        }

        internal fun toMap(kilde: Inntektsopplysningskilde): Map<String, Any?> = mapOf(
            "dato" to dato,
            "hendelseId" to hendelseId,
            "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
            "kilde" to kilde,
            "tidsstempel" to tidsstempel,

            "måned" to måned,
            "type" to type,
            "fordel" to fordel,
            "beskrivelse" to beskrivelse
        )
    }

    internal fun append(block: AppendMode.() -> Unit) {
        AppendMode(innslag).append(block)
    }

    internal class AppendMode(private val innslag: Innslag) {
        internal fun append(appender: AppendMode.() -> Unit) {
            apply(appender)
            skatt.takeIf { it.isNotEmpty() }?.also { add(SkattComposite(UUID.randomUUID(), it)) }
        }

        private val tidsstempel = LocalDateTime.now()
        private val skatt = mutableListOf<Skatt>()

        internal fun addSaksbehandler(dato: LocalDate, hendelseId: UUID, beløp: Inntekt) =
            add(Saksbehandler(UUID.randomUUID(), dato, hendelseId, beløp, tidsstempel))

        internal fun addInntektsmelding(dato: LocalDate, hendelseId: UUID, beløp: Inntekt) =
            add(Inntektsmelding(UUID.randomUUID(), dato, hendelseId, beløp, tidsstempel))

        internal fun addInfotrygd(dato: LocalDate, hendelseId: UUID, beløp: Inntekt) =
            add(Infotrygd(UUID.randomUUID(), dato, hendelseId, beløp, tidsstempel))

        internal fun addSkattSykepengegrunnlag(
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String
        ) =
            skatt.add(Skatt.Sykepengegrunnlag(dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel))

        internal fun addSkattSammenligningsgrunnlag(
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String
        ) =
            skatt.add(Skatt.Sammenligningsgrunnlag(dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel))

        private fun add(opplysning: Inntektsopplysning) {
            innslag.add(opplysning)
        }
    }

    internal fun restore(block: RestoreJsonMode.() -> Unit) {
        RestoreJsonMode(this).apply(block)
    }

    internal class RestoreJsonMode(private val inntektshistorikk: Inntektshistorikk) {
        internal fun innslag(innslagId: UUID, block: InnslagAppender.() -> Unit) {
            Innslag(innslagId).also { InnslagAppender(it).apply(block) }.also { inntektshistorikk.historikk.add(0, it) }
        }

        internal class InnslagAppender(private val innslag: Innslag) {
            internal fun add(opplysning: Inntektsopplysning) = innslag.add(opplysning)
        }
    }
}

private fun YearMonth.isWithinRangeOf(dato: LocalDate, måneder: Long) =
    this in YearMonth.from(dato).let { it.minusMonths(måneder)..it.minusMonths(1) }
