package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdrag
import java.time.LocalDate
import java.util.*

class Simulering(
    meldingsreferanseId: UUID,
    private val vedtaksperiodeId: String,
    aktørId: String,
    fødselsnummer: String,
    orgnummer: String,
    private val fagsystemId: String,
    fagområde: String,
    private val simuleringOK: Boolean,
    private val melding: String,
    internal val simuleringResultat: SimuleringResultat?,
    private val utbetalingId: UUID
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, orgnummer) {

    private val fagområde = Fagområde.from(fagområde)

    internal fun erRelevant(other: UUID) = other.toString() == vedtaksperiodeId
    internal fun erRelevantForUtbetaling(utbetalingId: UUID) = this.utbetalingId == utbetalingId
    internal fun erRelevantFor(fagområde: Fagområde, fagsystemId: String) = this.fagområde == fagområde && this.fagsystemId == fagsystemId

    internal fun valider(oppdrag: Oppdrag) = this.apply {
        if (!oppdrag.erRelevant(fagsystemId, fagområde)) return@apply
        if (!simuleringOK) {
            info("Feil under simulering: $melding")
            funksjonellFeil("Feil under simulering")
        }
        if (simuleringResultat == null) info("Ingenting ble simulert")
    }

    class SimuleringResultat(
        internal val totalbeløp: Int,
        internal val perioder: List<SimulertPeriode>
    ) {
        fun toMap() = mapOf(
            "totalbeløp" to totalbeløp,
            "perioder" to perioder.map { periode ->
                mapOf(
                    "fom" to periode.periode.start,
                    "tom" to periode.periode.endInclusive,
                    "utbetalinger" to periode.utbetalinger.map { utbetaling ->
                        mapOf(
                            "forfallsdato" to utbetaling.forfallsdato,
                            "utbetalesTil" to mapOf(
                                "id" to utbetaling.utbetalesTil.id,
                                "navn" to utbetaling.utbetalesTil.navn
                            ),
                            "feilkonto" to utbetaling.feilkonto,
                            "detaljer" to utbetaling.detaljer.map { detalj ->
                                mapOf(
                                    "fom" to detalj.periode.start,
                                    "tom" to detalj.periode.endInclusive,
                                    "konto" to detalj.konto,
                                    "beløp" to detalj.beløp,
                                    "klassekode" to mapOf(
                                        "kode" to detalj.klassekode.kode,
                                        "beskrivelse" to detalj.klassekode.beskrivelse
                                    ),
                                    "uføregrad" to detalj.uføregrad,
                                    "utbetalingstype" to detalj.utbetalingstype,
                                    "tilbakeføring" to detalj.tilbakeføring,
                                    "sats" to mapOf(
                                        "sats" to detalj.sats.sats,
                                        "antall" to detalj.sats.antall,
                                        "type" to detalj.sats.type
                                    ),
                                    "refunderesOrgnummer" to detalj.refunderesOrgnummer
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    class SimulertPeriode(
        internal val periode: Periode,
        internal val utbetalinger: List<SimulertUtbetaling>
    )

    class SimulertUtbetaling(
        internal val forfallsdato: LocalDate,
        internal val utbetalesTil: Mottaker,
        internal val feilkonto: Boolean,
        internal val detaljer: List<Detaljer>
    )

    class Detaljer(
        internal val periode: Periode,
        internal val konto: String,
        internal val beløp: Int,
        internal val klassekode: Klassekode,
        internal val uføregrad: Int,
        internal val utbetalingstype: String,
        internal val tilbakeføring: Boolean,
        internal val sats: Sats,
        internal val refunderesOrgnummer: String
    )

    class Sats(
        internal val sats: Double,
        internal val antall: Int,
        internal val type: String
    )

    class Klassekode(
        internal val kode: String,
        internal val beskrivelse: String
    )

    class Mottaker(
        internal val id: String,
        internal val navn: String
    )
}
