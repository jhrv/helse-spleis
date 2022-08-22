package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import no.nav.helse.assertForventetFeil
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereTest : AbstractDslTest() {

    @Test
    fun `kort sykdom hos ag2 med eksisterende vedtak`() {
        a1 { nyttVedtak(1.januar, 31.januar, 100.prosent) }
        nyPeriode(1.februar til 14.februar, a1, a2)
        a1 {
            assertEquals(1.januar, inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.skjæringstidspunkt)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a2 {
            assertEquals(1.januar, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.skjæringstidspunkt)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }

    }

    @Test
    fun `kort sykdom hos ag2`() {
        nyPeriode(1.januar til 14.januar, a1, a2)
        a1 {
            håndterSykmelding(Sykmeldingsperiode(15.januar, 31.januar, 100.prosent))
            håndterUtbetalingshistorikk(1.vedtaksperiode)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(15.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterYtelser(2.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `Sammenligningsgrunnlag for flere arbeidsgivere`() {
        val periodeA1 = 1.januar til 31.januar
        nyPeriode(1.januar til 31.januar, a1)
        a1 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(periodeA1.start til periodeA1.start.plusDays(15)),
                beregnetInntekt = 30000.månedlig
            )
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntektsvurdering = Inntektsvurdering(
                    inntekter = inntektperioderForSammenligningsgrunnlag {
                        1.januar(2017) til 1.juni(2017) inntekter {
                            a1 inntekt TestPerson.INNTEKT
                            a2 inntekt 5000.månedlig
                        }
                        1.august(2017) til 1.desember(2017) inntekter {
                            a1 inntekt 17000.månedlig
                            a2 inntekt 3500.månedlig
                        }
                    }
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = emptyList(),
                    arbeidsforhold = emptyList()
                )
            )
        }

        val periodeA2 = 15.januar til 15.februar
        a2 {
            nyPeriode(periodeA2)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(periodeA2.start til periodeA2.start.plusDays(15)),
                beregnetInntekt = 10000.månedlig

            )
        }
        a1 {
            assertEquals(
                318500.årlig,
                inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sammenligningsgrunnlag
            )
        }
    }

    @Test
    fun `Sammenligningsgrunnlag for flere arbeidsgivere som overlapper hverandres sykeperioder`() {
        a1 {
            nyPeriode(15.januar til 5.februar)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(15.januar til 28.januar),
                beregnetInntekt = 30000.månedlig
            )
        }
        a2 { nyPeriode(15.januar til 15.februar) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                vedtaksperiodeId = 1.vedtaksperiode,
                inntektsvurdering = Inntektsvurdering(
                    inntekter = inntektperioderForSammenligningsgrunnlag {
                        1.januar(2017) til 1.juni(2017) inntekter {
                            a1 inntekt TestPerson.INNTEKT
                            a2 inntekt 5000.månedlig
                        }
                        1.august(2017) til 1.desember(2017) inntekter {
                            a1 inntekt 17000.månedlig
                            a2 inntekt 3500.månedlig
                        }
                    }
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = emptyList(),
                    arbeidsforhold = emptyList()
                )
            )
        }
        a2 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(15.januar til 15.januar.plusDays(15)),
                beregnetInntekt = 10000.månedlig
            )
        }

        a1 {
            assertEquals(
                318500.årlig,
                inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sammenligningsgrunnlag
            )
        }
    }

    @Test
    fun `vedtaksperioder atskilt med betydelig tid`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            assertIngenFunksjonelleFeil()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            nyttVedtak(1.mars, 31.mars)
            assertIngenFunksjonelleFeil()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Tillater førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding er på samme dato`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        a1 { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent)) }

        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
            håndterUtbetalingshistorikk(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        }

        a2 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent)) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK) }

        a1 {
            håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
            håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)))
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt TestPerson.INNTEKT
                        a2 inntekt TestPerson.INNTEKT
                    }
                }
            ))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }

        a1 {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, TIL_UTBETALING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterYtelser(1.vedtaksperiode)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
            håndterSimulering(1.vedtaksperiode)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, TIL_UTBETALING)
            håndterUtbetalt()
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
    }

    @Test
    fun `Tillater førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding ikke er på samme dato - så lenge de er i samme måned`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        a1 { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent)) }

        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
            håndterUtbetalingshistorikk(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        }

        a2 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent))
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK) }

        a1 {
            håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 3.januar(2021), 6.januar(2021) til 18.januar(2021)),
                beregnetInntekt = 1000.månedlig
            )
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt TestPerson.INNTEKT
                        a2 inntekt 1000.månedlig
                    }
                }
            ))
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `Tillater ikke førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding ikke er på samme dato - hvis datoene er i forskjellig måned`() {
        val periode = 31.desember(2020) til 31.januar(2021)
        a1 { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent)) }
        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
            håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        }
        a2 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent)) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK) }
        a1 {
            håndterInntektsmelding(listOf(31.desember(2020) til 15.januar(2021))
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 3.januar(2021), 6.januar(2021) til 18.januar(2021)),
                beregnetInntekt = 1000.månedlig
            )
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt TestPerson.INNTEKT
                        a2 inntekt 1000.månedlig
                    }
                }
            ))
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

        }
        a2 { assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD) }
    }

    @Test
    fun `tillater to arbeidsgivere med korte perioder, og forlengelse av disse`() {
        val periode = 1.januar(2021) til 14.januar(2021)
        val forlengelseperiode = 15.januar(2021) til 31.januar(2021)
        nyPeriode(periode, a1, a2)
        nyPeriode(forlengelseperiode, a1, a2)
        a1 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),

            )
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),

            )
        }
        a1 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK) }
        a2 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt TestPerson.INNTEKT
                        a2 inntekt TestPerson.INNTEKT
                    }
                }
            ))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)

            håndterUtbetalt()
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
            håndterUtbetalt()
        }
        a1 { assertSisteTilstand(2.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(2.vedtaksperiode, AVSLUTTET) }
    }

    @Test
    fun `medlemskap ikke oppfyllt i vilkårsgrunnlag, avviser perioden riktig for begge arbeidsgivere`() {
        val periode = Periode(1.januar, 31.januar)
        nyPeriode(periode, a1, a2)
        a1 {
            håndterInntektsmelding(listOf(Periode(periode.start, periode.start.plusDays(15))))
        }
        a2 {
            håndterInntektsmelding(listOf(Periode(periode.start, periode.start.plusDays(15))))
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Nei,
                inntektsvurdering = Inntektsvurdering(
                    inntekter = inntektperioderForSammenligningsgrunnlag {
                        1.januar(2017) til 1.desember(2017) inntekter {
                            a1 inntekt TestPerson.INNTEKT
                        }
                        1.januar(2017) til 1.desember(2017) inntekter {
                            a2 inntekt TestPerson.INNTEKT
                        }
                    },
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = emptyList(),
                    arbeidsforhold = emptyList()
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsler()
        }
        a1 {
            inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }
        a2 {
            inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }

    }

    @Test
    fun `To arbeidsgivere med sykdom gir ikke warning for flere inntekter de siste tre månedene`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        a1 { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent)) }
        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent))
            håndterUtbetalingshistorikk(1.vedtaksperiode)
        }
        a2 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent)) }
        a1 { håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021))) }
        a2 { håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021))) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt TestPerson.INNTEKT
                        a2 inntekt TestPerson.INNTEKT
                    }
                }
            ))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()
            assertIngenVarsler()
        }
    }

    @Test
    fun `to AG - to perioder på hver - siste periode på første AG til godkjenning, siste periode på andre AG avventer første AG`() {
        nyPeriode(1.januar til 31.januar, a1, a2)
        a1 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
                beregnetInntekt = 20000.månedlig,

            )
        }
        a2 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
                beregnetInntekt = 20000.månedlig,

            )
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 20000.månedlig
                        a2 inntekt 20000.månedlig
                    }
                })
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        nyPeriode(1.februar til 28.februar, a1, a2)
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
        }
        a1 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertIngenFunksjonelleFeil()
            assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        a2 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING
            )
            assertIngenFunksjonelleFeil()
            assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(0, inspektør.avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(1, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }
    }

    @Test
    fun testyMc() {
        nyPeriode(1.januar til 31.januar, a1, a2)
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        a1 inntekt TestPerson.INNTEKT
                        a2 inntekt TestPerson.INNTEKT
                    }
                }
            ))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1.inspektør.utbetalinger.forEach {
            assertEquals(a1, it.inspektør.arbeidsgiverOppdrag.inspektør.mottaker)
        }
        a2.inspektør.utbetalinger.forEach {
            assertEquals(a2, it.inspektør.arbeidsgiverOppdrag.inspektør.mottaker)
        }
    }

    @Test
    fun `Går ikke direkte til AVVENTER_HISTORIKK dersom inntektsmelding kommer før søknad`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent)) }
        a1 {
            val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                AVVENTER_BLOKKERENDE_PERIODE,

            )
        }
    }

    @Test
    fun `Siste arbeidsgiver som går til AVVENTER_BLOKKERENDE_PERIODE sparker første tilbake til AVVENTER_HISTORIKK når inntektsmelding kommer før søknad`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent)) }
        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a2 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a1 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,

            )
        }
        a2 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                AVVENTER_BLOKKERENDE_PERIODE,

            )
        }
    }

    @Test
    fun `Skal ikke ha noen avviste dager ved ulik startdato selv om arbeidsgiverperiodedag og navdag overlapper og begge har sykdomsgrad på 20 prosent eller høyere`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 20.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(17.januar, 16.februar, 20.prosent)) }
        a1 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 20.prosent)) }
        a2 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(17.januar, 16.februar, 20.prosent)) }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(17.januar til 1.februar)) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntektsvurdering = Inntektsvurdering(
                    inntekter = inntektperioderForSammenligningsgrunnlag {
                        1.januar(2017) til 1.desember(2017) inntekter {
                            a1 inntekt TestPerson.INNTEKT
                            a2 inntekt TestPerson.INNTEKT
                        }
                    }
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = inntektperioderForSykepengegrunnlag {
                        1.oktober(2017) til 1.desember(2017) inntekter {
                            a1 inntekt TestPerson.INNTEKT
                            a2 inntekt TestPerson.INNTEKT
                        }
                    }, arbeidsforhold = emptyList()
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertForventetFeil(
                forklaring = "Ønsket oppførsel: arbeidsgiverperiodedag må ha ekte grad (ikke 0%), da den teller med i beregning av total sykdomsgrad " +
                        "som kan slå ut negativt ved flere arbeidsgivere. Skjer eksempelvis dersom man beregner totalgrad av arbeidsgiverperiodedag hos én " +
                        "arbeidsgiver og sykedag med 20% sykdom hos en annen arbeidsgiver",
                nå = {
                    Assertions.assertTrue(a1.inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistDagTeller > 0)
                },
                ønsket = {
                    assertEquals(
                        0,
                        a1.inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistDagTeller
                    )
                }
            )
        }
    }

    @Test
    fun `Sykmelding og søknad kommer for to perioder før inntektsmelding kommer - skal fortsatt vilkårsprøve kun én gang`() {
        nyPeriode(1.januar til 18.januar, a1, a2)
        a1 { håndterSykmelding(Sykmeldingsperiode(20.januar, 31.januar, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent)) }
        a1 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(20.januar, 31.januar, 100.prosent)) }
        a2 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(22.januar, 31.januar, 100.prosent)) }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 20.januar) }
        // Sender med en annen inntekt enn i forrige IM for å kunne asserte på at det er denne vi bruker
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 32000.månedlig, førsteFraværsdag = 22.januar) }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 31000.månedlig) }
        val sammenligningsgrunnlag = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, 20.januar, TestPerson.INNTEKT.repeat(12)),
                sammenligningsgrunnlag(a2, 20.januar, 32000.månedlig.repeat(12))
            )
        )
        val sykepengegrunnlag = InntektForSykepengegrunnlag(
            inntekter = listOf(
                grunnlag(a1, 20.januar, TestPerson.INNTEKT.repeat(3)),
                grunnlag(a2, 20.januar, TestPerson.INNTEKT.repeat(3))
            ), arbeidsforhold = emptyList()
        )
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntektsvurdering = sammenligningsgrunnlag,
                inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag,

            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 { assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK) }
        a2 { assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `Burde ikke håndtere sykmelding dersom vi har forkastede vedtaksperioder i andre arbeidsforhold`() {
        a1 { nyPeriode(2.januar til 1.februar) }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            nyPeriode(1.januar til 31.januar)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `kastes ikke ut pga manglende inntekt etter inntektsmelding`() {
        a2 { håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent)) }
        a1 { håndterSykmelding(Sykmeldingsperiode(4.januar, 18.januar, 100.prosent)) }
        a2 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 18.januar, 100.prosent))
            håndterUtbetalingshistorikk(1.vedtaksperiode)
        }
        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 18.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(19.januar, 22.januar, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(19.januar, 22.januar, 100.prosent))
            håndterInntektsmelding(listOf(4.januar til 19.januar))
            håndterYtelser(2.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            assertIngenFunksjonelleFeil(2.vedtaksperiode.filter())
            assertEquals(Inntektskilde.EN_ARBEIDSGIVER, a1.inspektør.inntektskilde(2.vedtaksperiode))
        }
        a2 {
            val vilkårsgrunnlag = a2.inspektør.vilkårsgrunnlag(1.vedtaksperiode)
            assertNotNull(vilkårsgrunnlag)
            Assertions.assertTrue(vilkårsgrunnlag.inspektør.vurdertOk)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING) }
        a1 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING) }
    }

    @Test
    fun `går til AVVENTER_BLOKKERENDE_PERIODE ved IM dersom vi har vedtaksperioder som ikke overlapper, men har samme skjæringstidspunkt som nåværende`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent)) }
        a3 { håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent)) }
        a1 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent)) }
        a2 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent)) }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }

        assertForventetFeil(
            forklaring = "https://trello.com/c/vVcsM2tp " +
                    "Hvis vi mottar inntektsmelding for ag1 og ag2 vil koden i dag hoppe videre til AVVENTER_HISTORIKK " +
                    "siden alle perioder som overlapper har inntekt. " +
                    "Vi burde forsikre oss om at alle vedtaksperioder som har samme skjæringstidspunkt har inntekt i stedet.",
            nå = {
                a1 { assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK) }
                a2 { assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
            },
            ønsket = {
                a1 { assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
                a2 { assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
            }
        )
        // TODO: Utvid testen med IM og utbetaling for AG3
    }

    @Test
    fun `forlengelse av AVSLUTTET_UTEN_UTBETALING skal ikke gå til AVVENTER_HISTORIKK ved flere arbeidsgivere om IM kommer først`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent)) }
        a1 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 16.januar, 100.prosent)) }
        a2 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 16.januar, 100.prosent)) }
        a1 { håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a1 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(17.januar, 31.januar, 100.prosent)) }
        a2 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(17.januar, 31.januar, 100.prosent)) }
        a1 {
            assertTilstander(1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
            )
            assertTilstander(2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
            )
        }
        a2 {
            assertTilstander(1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                AVVENTER_BLOKKERENDE_PERIODE
            )
            assertTilstander(2.vedtaksperiode, START, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `GjenopptaBehandling poker ikke fremtidig periode for en annen arbeidsgiver videre ved tidligere uferdige perioder`() {
        a1 {
            nyPeriode(1.januar til 31.januar)
            nyPeriode(1.februar til 28.februar)
        }
        a2 {
            nyPeriode(1.mai til 31.mai)
            håndterInntektsmelding(listOf(1.mai til 16.mai))
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 { assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
        }
        a1 { assertSisteTilstand(2.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING) }
    }

    @Test
    fun `inntektsmelding skal kun treffe sammenhengende vedtaksperioder, ikke alle med samme skjæringstidspunkt`() {
        // Vedtaksperiode for AG 1 skal bare koble sammen to vedtaksperioder for AG 2 så de får samme skjæringstidspunkt
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent)) }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 20.januar, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(25.januar, 31.januar, 100.prosent))
        }
        a1 { håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a1 { assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK) }
        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        }
    }

    @Test
    fun `Burde ikke kunne opprette vedtaksperiode før utbetalt periode ved flere AG`() {
        a2 { nyttVedtak(1.mars, 31.mars) }
        a1 { nyPeriode(1.januar til 31.januar) }
        assertForventetFeil(
            forklaring = "Perioden blir ikke forkastet dersom en vedtaksperiode er utbetalt for en annen arbeidsgiver",
            nå = {
                Assertions.assertFalse(a1.inspektør.periodeErForkastet(1.vedtaksperiode(a1)))
            },
            ønsket = {
                Assertions.assertTrue(a1.inspektør.periodeErForkastet(1.vedtaksperiode(a1)))
            }
        )
    }
}