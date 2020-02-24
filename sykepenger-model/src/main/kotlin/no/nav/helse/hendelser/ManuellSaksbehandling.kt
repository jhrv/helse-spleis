package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDateTime

class ManuellSaksbehandling(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    internal val vedtaksperiodeId: String,
    private val saksbehandler: String,
    private val utbetalingGodkjent: Boolean,
    private val godkjenttidspunkt: LocalDateTime? = null
) : ArbeidstakerHendelse() {

    internal fun saksbehandler() = saksbehandler
    internal fun utbetalingGodkjent() = utbetalingGodkjent
    internal fun godkjenttidspunkt() = godkjenttidspunkt

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer
    override fun melding(klassName: String) = "Manuell Saksbehandling"
}
