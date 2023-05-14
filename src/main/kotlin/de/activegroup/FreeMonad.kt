package de.activegroup

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

object FreeMonad {

    class ContextElement<FA>(var contents: FA?) : AbstractCoroutineContextElement(ContextElement) {
        companion object Key : CoroutineContext.Key<ContextElement<*>>
    }

    fun <DSL, FA, A> effect(dsl: DSL,block: suspend DSL.() -> A, coroutineContext: CoroutineContext = EmptyCoroutineContext): FA {
        val element = ContextElement<FA>(null)
        suspend { dsl.block() }.startCoroutine(
            Continuation(coroutineContext + element) { result ->
                result.onFailure { exception ->
                    val currentThread = Thread.currentThread()
                    currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
                }
            }
        )
        return element.contents!!
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <FA, A> pure(result: A, pure: (A) -> FA): A =
        suspendCoroutine {
            val element = it.context[ContextElement]!! as ContextElement<FA>
            element.contents = pure(result)
            it.resume(result)
        }

    @Suppress("UNCHECKED_CAST")
    suspend fun <FA, A> susp(bind: ((A) -> FA) -> FA): A =
        suspendCoroutine {
            val element = it.context[ContextElement]!! as ContextElement<FA>
            element.contents =
                bind { result ->
                    it.resume(result)
                    element.contents!!
                }
        }
}
