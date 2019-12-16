package no.nav.helse.hendelser.søknad

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.SykdomshendelseType
import no.nav.helse.sak.UtenforOmfangException
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.util.*

class NySøknadHendelse private constructor(hendelseId: String, søknad: JsonNode) : SøknadHendelse(hendelseId, SykdomshendelseType.NySøknadMottatt, søknad) {

    constructor(søknad: JsonNode) : this(UUID.randomUUID().toString(), søknad)

    companion object {
        fun fromJson(jsonNode: JsonNode): NySøknadHendelse {
            return NySøknadHendelse(jsonNode["hendelseId"].textValue(), jsonNode["søknad"])
        }
    }

    override fun kanBehandles(): Boolean {
        return super.kanBehandles() && sykeperioder.all { it.sykmeldingsgrad == 100 }
    }

    private val sykeperiodeTidslinje
        get(): List<ConcreteSykdomstidslinje> = sykeperioder
                .map { ConcreteSykdomstidslinje.sykedager(it.fom, it.tom, this) }

    override fun sykdomstidslinje() =
            sykeperiodeTidslinje.reduce { resultatTidslinje, delTidslinje ->
                if (resultatTidslinje.overlapperMed(delTidslinje)) throw UtenforOmfangException("Søknaden inneholder overlappende sykdomsperioder", this)
                resultatTidslinje + delTidslinje
            }

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Sykmelding
}
