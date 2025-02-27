package no.nav.helse.serde.api.dto

import no.nav.helse.utbetalingslinjer.Endringskode

enum class EndringskodeDTO {
    NY, UEND, ENDR;

    internal companion object {
        internal fun Endringskode.dto() = when (this) {
            Endringskode.NY -> NY
            Endringskode.ENDR -> ENDR
            Endringskode.UEND -> UEND
        }
    }
}