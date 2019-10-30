package no.nav.helse.inntektshistorikk

import no.nav.helse.behov.Behov
import no.nav.helse.hendelse.SakskompleksHendelse
import no.nav.helse.person.domain.PersonHendelse

class InntektshistorikkHendelse(private val behov: Behov) : PersonHendelse, SakskompleksHendelse {

    fun avvikSisteTreMåneder(): Boolean {
        val løsning = behov.løsning() as Map<*, *>
        return (løsning["avvikSisteTreMåneder"] as Boolean?)!!
    }

    override fun aktørId(): String {
        return behov["aktørId"]!!
    }

    override fun organisasjonsnummer(): String? {
        return behov["organisasjonsnummer"]
    }

    override fun sakskompleksId(): String {
        return behov["sakskompleksId"]!!
    }
}
