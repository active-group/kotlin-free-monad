package de.activegroup

sealed interface Reader<R, out A> {
    fun <B> bind(next: (A) -> Reader<R, B>): Reader<R, B>
    data class Ask<R, out A>(val cont: (R) -> Reader<R, A>): Reader<R, A> {
        override fun <B> bind(next: (A) -> Reader<R, B>): Reader<R, B> =
            Ask { r -> cont(r).bind(next) }
    }
    data class Pure<R, out A>(val result : A): Reader<R, A> {
        override fun <B> bind(next: (A) -> Reader<R, B>): Reader<R, B> =
            next(result)
    }

    suspend fun susp(): A = MonadDSL.susp<Reader<R, A>, A>(this::bind)

    fun run(r: R): A = run(this, r)

    companion object {
        fun <R, A> reader(block: suspend ReaderDsl<R>.() -> A): Reader<R, A> = MonadDSL.effect(ReaderDsl(), block)

        tailrec fun <R, A> run(reader: Reader<R, A>, r: R): A =
            when (reader) {
                is Ask -> run(reader.cont(r), r)
                is Pure -> reader.result
            }
    }
}

open class ReaderDsl<R> {
    suspend fun ask(): R = Reader.Ask<R, R> { Reader.Pure(it) }.susp()
    suspend fun <A> pure(result: A): A = MonadDSL.pure<Reader<R, A>, A>(result) { Reader.Pure(it)  }
}

