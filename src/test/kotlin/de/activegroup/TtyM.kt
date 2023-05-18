package de.activegroup

sealed interface TtyM<out A> {
    data class Write<out A>(val text: String, val cont: (Unit) -> TtyM<A>): TtyM<A> {
        override fun <B> bind(next: (A) -> TtyM<B>): TtyM<B> =
            Write(text) { unit -> cont(unit).bind(next) }
    }
    data class Pure<out A>(val result: A): TtyM<A> {
        override fun <B> bind(next: (A) -> TtyM<B>): TtyM<B> =
            next(result)
    }
    fun <B> bind(next: (A) -> TtyM<B>): TtyM<B>

    suspend fun susp(): A = MonadDSL.susp<TtyM<A>, A>(this::bind)

    companion object {
        tailrec suspend fun <A> run(tty: TtyM<A>): A =
            when (tty) {
                is Write -> {
                    println(tty.text)
                    run(tty.cont(Unit))
                }
                is Pure -> tty.result
            }

        tailrec suspend fun <A> output(tty: TtyM<A>, output: MutableList<String>): A =
            when (tty) {
                is Write -> {
                    output.add(tty.text)
                    output(tty.cont(Unit), output)
                }
                is Pure -> tty.result
            }
    }
}


object TtyDsl {
    suspend fun write(text: String) = TtyM.Write(text) { TtyM.Pure(it) }.susp()
    suspend fun  <A> pure(result: A) = MonadDSL.pure(result) { TtyM.Pure(it) }
    fun <A> effect(block: suspend TtyDsl.() -> A): TtyM<A> = MonadDSL.effect(this, block)
}