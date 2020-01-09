package no.nav.helse.spleis

import io.prometheus.client.Counter
import no.nav.helse.hendelser.*
import no.nav.helse.person.ArbeidstakerHendelse
import org.slf4j.LoggerFactory

class HendelseProbe: HendelseListener {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")
        private val log = LoggerFactory.getLogger(HendelseProbe::class.java)

        private val hendelseCounter = Counter.build("hendelser_totals", "Antall hendelser mottatt")
            .labelNames("type")
            .register()

        private val påminnetCounter =
            Counter.build("paminnet_totals", "Antall ganger vi har mottatt en påminnelse")
                .labelNames("tilstand")
                .register()
    }

    override fun onPåminnelse(påminnelse: Påminnelse) {
        påminnetCounter
            .labels(påminnelse.tilstand.toString())
            .inc()
        påminnelse.tell()
    }

    override fun onYtelser(ytelser: Ytelser) {
        ytelser.tell()
    }

    override fun onManuellSaksbehandling(manuellSaksbehandling: ManuellSaksbehandling) {
        log.info("Mottatt svar på manuell saksbehandling. Godkjent:${manuellSaksbehandling.utbetalingGodkjent()} for vedtaksperiode:${manuellSaksbehandling.vedtaksperiodeId()}")
        manuellSaksbehandling.tell()
    }

    override fun onVilkårsgrunnlag(vilkårsgrunnlag: Vilkårsgrunnlag) {
        vilkårsgrunnlag.tell()
    }

    override fun onInntektsmelding(inntektsmelding: Inntektsmelding) {
        inntektsmelding.tell()
    }

    override fun onNySøknad(søknad: NySøknad) {
        søknad.tell()
    }

    override fun onSendtSøknad(søknad: SendtSøknad) {
        søknad.tell()
    }

    override fun onUnprocessedMessage(message: String) {
        sikkerLogg.info("uhåndtert melding $message")
    }

    private fun ArbeidstakerHendelse.tell() {
        hendelseCounter.labels(this.hendelsetype().name).inc()
    }
}
