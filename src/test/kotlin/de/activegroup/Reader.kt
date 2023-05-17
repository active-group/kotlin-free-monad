package de.activegroup

import kotlinx.coroutines.runBlocking

sealed interface Reader<R, out A> {
    fun <B> bind(next: (A) -> Reader<R, B>): Reader<R, B>
    data class Ask<R, out A>(val cont: (R) -> Reader<R, A>): Reader<R, A> {
        override fun <B> bind(next: (A) -> Reader<R, B>): Reader<R, B> =
            Ask { r -> cont(r).bind(next) }
    }
    data class Pure<out A>(val result : A): Reader<Nothing, A> {
        override fun <B> bind(next: (A) -> Reader<Nothing, B>): Reader<Nothing, B> =
            next(result)
    }

    suspend fun susp(): A = MonadDSL.susp<Reader<R, A>, A>(this::bind)

    companion object {
        fun <R, A> reader(block: suspend ReaderDsl.() -> A): Reader<R, A> = MonadDSL.effect(ReaderDsl(), block)

        tailrec suspend fun <R, A> run(reader: Reader<R, A>, r: R): A =
            when (reader) {
                is Ask -> run(reader.cont(r), r)
                is Pure -> reader.result
            }
    }
}

fun <R, A> Reader<R, A>.run(r: R): A {
    val reader = this
    return runBlocking { Reader.Companion.run(reader, r) }
}

class ReaderDsl() {
    suspend fun <B> ask(): B = Reader.Ask<B, B> { Reader.Pure(it) as Reader<B, B> }.susp()
    suspend fun <A> pure(result: A) = MonadDSL.pure(result) { Reader.Pure(it) }
}