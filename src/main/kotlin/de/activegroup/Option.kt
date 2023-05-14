package de.activegroup

sealed interface Option<out A> {
    companion object {
        fun <A> some(value: A): Option<A> = Some(value)
        fun <A> none(): Option<A> = None

        fun <A> Option<A>.get(): A =
            when (this) {
                is None -> throw AssertionError("found None where Some was expected")
                is Some -> value
            }
    }
}

object None : Option<Nothing> {}
data class Some<out A>(val value: A) : Option<A> {}
