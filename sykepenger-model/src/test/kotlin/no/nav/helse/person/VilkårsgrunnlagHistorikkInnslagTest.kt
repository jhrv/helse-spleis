package no.nav.helse.person

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagHistorikkInnslagTest {
    private lateinit var innslag: VilkårsgrunnlagHistorikk.Innslag

    private companion object {
        const val UNG_PERSON_FNR_2018 = "12029240045"
        val ALDER = Fødselsnummer.tilFødselsnummer(UNG_PERSON_FNR_2018).alder()
    }

    @BeforeEach
    fun beforeEach() {
        innslag = VilkårsgrunnlagHistorikk.Innslag(UUID.randomUUID(), LocalDateTime.now())
    }

    @Test
    fun `finner ikke begrunnelser dersom vilkårsgrunnlag ikke er Grunnlagsdata`() {
        val innslag = VilkårsgrunnlagHistorikk.Innslag(UUID.randomUUID(), LocalDateTime.now())
        innslag.add(1.januar, testgrunnlag)
        assertEquals(0, innslag.finnBegrunnelser(ALDER).size)
    }

    @Test
    fun `finner kun begrunnelser fra vilkårsgrunnlag som er Grunnlagsdata`() {
        innslag.add(1.januar, testgrunnlag)
        innslag.add(1.januar, grunnlagsdata(1.januar, vurdertOk = false, harOpptjening = false))
        val begrunnelserMap = innslag.finnBegrunnelser(ALDER)
        assertEquals(1, begrunnelserMap.size)
        assertEquals(listOf(Begrunnelse.ManglerOpptjening), begrunnelserMap[1.januar])
    }

    @Test
    fun `finner begrunnelser dersom vilkårsgrunnlag er Grunnlagsdata`() {
        innslag.add(1.januar, grunnlagsdata(1.januar, vurdertOk = false, harOpptjening = false))
        val begrunnelserMap = innslag.finnBegrunnelser(ALDER)
        assertEquals(1, begrunnelserMap.size)
        assertEquals(listOf(Begrunnelse.ManglerOpptjening), begrunnelserMap[1.januar])
    }

    @Test
    fun `finner ikke begrunnelser dersom vilkårsgrunnlag er Grunnlagsdata og grunnlaget er vurdert ok`() {
        innslag.add(1.januar, grunnlagsdata(1.januar, vurdertOk = true))
        assertEquals(0, innslag.finnBegrunnelser(ALDER).size)
    }

    @Test
    fun `finner alle begrunnelser dersom vilkårsgrunnlag er Grunnlagsdata`() {
        innslag.add(1.januar, grunnlagsdata(1.januar, vurdertOk = false, harMinimumInntekt = false, harOpptjening = false))
        val begrunnelserMap = innslag.finnBegrunnelser(ALDER)
        assertEquals(1, begrunnelserMap.size)
        assertEquals(listOf(Begrunnelse.MinimumInntekt, Begrunnelse.ManglerOpptjening), begrunnelserMap[1.januar])
    }

    @Test
    fun `finner begrunnelser ved flere grunnlagsdata`() {
        innslag.add(1.januar, grunnlagsdata(1.januar, vurdertOk = false, harOpptjening = false))
        innslag.add(2.januar, grunnlagsdata(2.januar, vurdertOk = false, harOpptjening = false))
        val begrunnelserMap = innslag.finnBegrunnelser(ALDER)
        assertEquals(2, begrunnelserMap.size)
        assertEquals(listOf(Begrunnelse.ManglerOpptjening), begrunnelserMap[1.januar])
        assertEquals(listOf(Begrunnelse.ManglerOpptjening), begrunnelserMap[2.januar])
    }

    private fun grunnlagsdata(skjæringstidspunkt: LocalDate, vurdertOk: Boolean = true, harOpptjening: Boolean = true, harMinimumInntekt: Boolean = true) =
        VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = Sykepengegrunnlag(emptyList(), skjæringstidspunkt, Aktivitetslogg()),
            sammenligningsgrunnlag = Inntekt.INGEN,
            avviksprosent = 0.0.prosent,
            antallOpptjeningsdagerErMinst = 28,
            harOpptjening = harOpptjening,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            harMinimumInntekt = harMinimumInntekt,
            vurdertOk = vurdertOk,
            meldingsreferanseId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )

    private val testgrunnlag
        get() = object : VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement {
            override fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {}

            override fun sykepengegrunnlag() = Inntekt.INGEN

            override fun grunnlagsBegrensning() = Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET

            override fun grunnlagForSykepengegrunnlag() = Inntekt.INGEN

            override fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> = emptyMap()

            override fun gjelderFlereArbeidsgivere() = false
            override fun begrunnelserForAvvisteVilkår(alder: Alder, skjæringstidspunkt: LocalDate): List<Begrunnelse> = emptyList()
            override fun skjæringstidspunkt() = 1.januar

            override fun valider(aktivitetslogg: Aktivitetslogg) {}
        }
}
