package de.activegroup

sealed interface Option<out A> {
    fun <B> bind(next: (A) -> Option<B>): Option<B>
    suspend fun susp(): A = MonadDSL.susp<Option<A>, A>(this::bind)

    companion object {
        fun <A> some(value: A): Option<A> = Some(value)
        fun <A> none(): Option<A> = None

        fun <A> Option<A>.get(): A =
            when (this) {
                is None -> throw AssertionError("found None where Some was expected")
                is Some -> value
            }

        fun <A> optionally(block: suspend OptionDSL.() -> A): Option<A> =
            MonadDSL.effect(OptionDSL(), block)
    }
}

object None : Option<Nothing> {
    override fun <B> bind(next: (Nothing) -> Option<B>): Option<B> = None
}
data class Some<out A>(val value: A) : Option<A> {
    override fun <B> bind(next: (A) -> Option<B>): Option<B> = next(this.value)
}

class OptionDSL() {
    suspend fun  <A> pure(result: A): A = MonadDSL.pure(result) { Some(it) }
}
