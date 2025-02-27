package no.nav.helse

import java.util.UUID
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.IdInnhenter
import no.nav.helse.serde.reflection.castAsList

internal fun IAktivitetslogg.antallEtterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype, orgnummer: String = AbstractPersonTest.ORGNUMMER) =
    antallEtterspurteBehov(vedtaksperiodeIdInnhenter.id(orgnummer), behovtype)

internal fun IAktivitetslogg.etterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype, orgnummer: String = AbstractPersonTest.ORGNUMMER) =
    etterspurteBehovFinnes(vedtaksperiodeIdInnhenter.id(orgnummer), behovtype)

internal fun IAktivitetslogg.etterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String = AbstractPersonTest.ORGNUMMER) =
    etterspurteBehov(vedtaksperiodeIdInnhenter.id(orgnummer))

internal fun IAktivitetslogg.sisteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String = AbstractPersonTest.ORGNUMMER) =
    behov().last { it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeIdInnhenter.id(orgnummer).toString() }

internal fun IAktivitetslogg.sisteBehov(vedtaksperiodeId: UUID) =
    behov().last { it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString() }

internal fun IAktivitetslogg.sisteBehov(type: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
    behov().last { it.type == type }

internal fun IAktivitetslogg.harBehov(behov: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
    this.behov().any { it.type == behov }

internal fun IAktivitetslogg.antallEtterspurteBehov(vedtaksperiodeId: UUID, behov: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.count { it.type == behov }

internal fun IAktivitetslogg.etterspurteBehovFinnes(vedtaksperiodeId: UUID, behov: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.any { it.type == behov }

internal fun IAktivitetslogg.etterspurteBehov(vedtaksperiodeId: UUID) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }

internal fun IAktivitetslogg.etterspurteBehov(vedtaksperiodeId: UUID, behov: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
    this.behov().filter {
        it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString()
    }.filter { it.type == behov }.size == 1

inline fun <reified T> IAktivitetslogg.etterspurtBehov(vedtaksperiodeId: UUID, behov: Aktivitetslogg.Aktivitet.Behov.Behovtype, felt: String): T? {
    return this.behov()
        .filter { it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeId.toString() }
        .first { it.type == behov }.detaljer()[felt] as T?
}

private fun IAktivitetslogg.hent(alvorlighetsgrad: String) = toMap()["aktiviteter"]
    .castAsList<Map<String, Any>>()
    .filter { it["alvorlighetsgrad"] == alvorlighetsgrad }
    .map { it["melding"] }
    .mapNotNull { it.toString() }

internal fun IAktivitetslogg.hentErrors() = hent("ERROR")
internal fun IAktivitetslogg.hentWarnings() = hent("WARN")
internal fun IAktivitetslogg.hentInfo() = hent("INFO")
