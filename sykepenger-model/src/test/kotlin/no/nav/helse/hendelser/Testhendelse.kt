package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDateTime
import java.util.*

internal class Testhendelse(
    private val rapportertdato: LocalDateTime = LocalDateTime.of(2019, 9, 16, 10, 45),
    private val hendelsetype: Dag.NøkkelHendelseType = Dag.NøkkelHendelseType.Søknad,
    aktivitetslogger: Aktivitetslogger = Aktivitetslogger()
) :
    SykdomstidslinjeHendelse(UUID.randomUUID(), Hendelsestype.SendtSøknad, aktivitetslogger) {
    override fun nøkkelHendelseType(): Dag.NøkkelHendelseType = hendelsetype

    override fun sykdomstidslinje(): ConcreteSykdomstidslinje {
        TODO("not implemented")
    }

    override fun aktørId(): String {
        TODO("not implemented")
    }

    override fun fødselsnummer(): String {
        TODO("not implemented")
    }

    override fun organisasjonsnummer(): String {
        TODO("not implemented")
    }

    override fun rapportertdato(): LocalDateTime {
        return rapportertdato
    }

    override fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        TODO("not implemented")
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver, person: Person) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun valider(): Aktivitetslogger {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun equals(other: Any?): Boolean = this === other

    override fun accept(visitor: PersonVisitor) {
    }
}
