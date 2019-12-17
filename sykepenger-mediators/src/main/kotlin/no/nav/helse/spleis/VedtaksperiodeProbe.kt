package no.nav.helse.spleis

import io.prometheus.client.Counter
import io.prometheus.client.Summary
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.SykdomshendelseType
import no.nav.helse.hendelser.inntektsmelding.Inntektsmelding
import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.hendelser.søknad.NySøknad
import no.nav.helse.hendelser.søknad.SendtSøknad
import no.nav.helse.sak.SakObserver
import no.nav.helse.sak.SakskjemaForGammelt
import no.nav.helse.sak.VedtaksperiodeObserver.StateChangeEvent
import org.slf4j.LoggerFactory

object VedtaksperiodeProbe : SakObserver {

    private val log = LoggerFactory.getLogger(VedtaksperiodeProbe::class.java)

    private val behovCounter = Counter.build("behov_totals", "Antall behov opprettet")
        .labelNames("behovType")
        .register()

    private val dokumenterKobletTilSakCounter = Counter.build(
        "dokumenter_koblet_til_sak_totals",
        "Antall inntektsmeldinger vi har mottatt som ble koblet til et vedtaksperiode"
    )
        .labelNames("dokumentType")
        .register()

    private val tilstandCounter = Counter.build(
        "vedtaksperiode_tilstander_totals",
        "Fordeling av tilstandene sakene er i, og hvilken tilstand de kom fra"
    )
        .labelNames("forrigeTilstand", "tilstand", "hendelse")
        .register()

    private val utenforOmfangCounter = Counter.build("utenfor_omfang_totals", "Antall ganger en sak er utenfor omfang")
        .labelNames("dokumentType")
        .register()

    private val vedtaksperiodePåminnetCounter =
        Counter.build("vedtaksperiode_paminnet_totals", "Antall ganger en vedtaksperiode er blitt påminnet")
            .labelNames("tilstand")
            .register()

    private val sakskjemaForGammeltCounter = Counter.build("sakskjema_for_gammelt_totals", "fordeling av versjonsnummer på sakskjema")
        .labelNames("skjemaVersjon")
        .register()

    private val sakMementoStørrelse =
        Summary.build("sak_memento_size", "størrelse på sak document i databasen").register()

    override fun vedtaksperiodeTrengerLøsning(event: Behov) {
        event.behovType().forEach { behovCounter.labels(it).inc() }
    }

    override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {
        sakMementoStørrelse.observe(sakEndretEvent.memento.toString().length.toDouble())
    }

    fun forGammelSkjemaversjon(err: SakskjemaForGammelt) {
        sakskjemaForGammeltCounter
            .labels("${err.skjemaVersjon}")
            .inc()
    }

    override fun vedtaksperiodeEndret(event: StateChangeEvent) {
        tilstandCounter.labels(
            event.forrigeTilstand.name,
            event.gjeldendeTilstand.name,
            event.sykdomshendelse.javaClass.simpleName
        ).inc()

        log.info("vedtaksperiode=${event.id} event=${event.sykdomshendelse.javaClass.simpleName} state=${event.gjeldendeTilstand} previousState=${event.forrigeTilstand}")

        when (event.sykdomshendelse) {
            is Inntektsmelding -> {
                dokumenterKobletTilSakCounter.labels(SykdomshendelseType.InntektsmeldingMottatt.name).inc()
            }
            is NySøknad -> {
                dokumenterKobletTilSakCounter.labels(SykdomshendelseType.NySøknadMottatt.name).inc()
            }
            is SendtSøknad -> {
                dokumenterKobletTilSakCounter.labels(SykdomshendelseType.SendtSøknadMottatt.name).inc()
            }
        }
    }

    override fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
        log.info(
            "mottok påminnelse nr.${påminnelse.antallGangerPåminnet}, sendt: ${påminnelse.påminnelsestidspunkt} for " +
                "vedtaksperiode: ${påminnelse.vedtaksperiodeId()} som gikk i tilstand: ${påminnelse.tilstand} på ${påminnelse.tilstandsendringstidspunkt}." +
                "Neste påminnelsetidspunkt er: ${påminnelse.nestePåminnelsestidspunkt}"
        )
        vedtaksperiodePåminnetCounter
            .labels(påminnelse.tilstand.toString())
            .inc()
    }

    fun utenforOmfang(hendelse: Any) {
        utenforOmfangCounter.labels(hendelse.javaClass.simpleName).inc()
    }

}
