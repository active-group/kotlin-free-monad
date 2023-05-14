package de.activegroup

import de.activegroup.Option.Companion.get
import de.activegroup.Option.Companion.none
import de.activegroup.Option.Companion.some
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

object FreeMonad {

    class ContextElement<FA>(var contents: Option<FA>) : AbstractCoroutineContextElement(ContextElement) {
        companion object Key : CoroutineContext.Key<ContextElement<*>>
    }

    fun <A, FA, DSL> effect(dsl: DSL,block: suspend DSL.() -> A, coroutineContext: CoroutineContext = EmptyCoroutineContext, ): FA {
        val element = ContextElement<FA>(none())
        suspend { dsl.block() }.startCoroutine(
            Continuation(coroutineContext + element) { result ->
                result.onFailure { exception ->
                    val currentThread = Thread.currentThread()
                    currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
                }
            }
        )
        return element.contents.get()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <A, FA> pure(result: A, pure: (A) -> FA): A =
        suspendCoroutine {
            val element = it.context[ContextElement]!! as ContextElement<FA>
            element.contents = some(pure(result))
            it.resume(result)
        }

    @Suppress("UNCHECKED_CAST")
    suspend fun <A, FA> susp(bind: ((A) -> FA) -> FA): A =
        suspendCoroutine {
            val element = it.context[FreeMonad.ContextElement]!! as FreeMonad.ContextElement<FA>
            element.contents = some(
                bind { result ->
                    it.resume(result)
                    element.contents.get()
                }
            )
        }
}
