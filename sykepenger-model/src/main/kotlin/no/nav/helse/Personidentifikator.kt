package no.nav.helse

class Personidentifikator private constructor(private val value: String) {
    override fun toString() = value
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = other is Personidentifikator && this.value == other.value
    fun toLong() = value.toLong()
    companion object {
        fun somPersonidentifikator(ident: String): Personidentifikator {
            if (ident.length == 11 && alleTegnErSiffer(ident)) return Personidentifikator(ident)
            else throw RuntimeException("$ident er ikke en gyldig identifikator.")
        }
        private fun alleTegnErSiffer(string: String) = string.matches(Regex("\\d*"))
    }
}

fun String.somPersonidentifikator() = Personidentifikator.somPersonidentifikator(this)
