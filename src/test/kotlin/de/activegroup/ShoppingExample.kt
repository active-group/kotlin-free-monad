package de.activegroup

import de.activegroup.Shopping.Companion.shopping
import kotlin.test.Test
import kotlin.test.assertEquals

data class Article(val id: Int, val name: String)

sealed interface Shopping<out A> {
    fun <B> bind(next: (A) -> Shopping<B>): Shopping<B>
    data class GetArticle<out A>(val id: Int, val cont: (Article) -> Shopping<A>): Shopping<A> {
        override fun <B> bind(next: (A) -> Shopping<B>): Shopping<B> =
            GetArticle(id) { article -> cont(article).bind(next) }
    }
    data class GetCustomer<out A>(val id: Int, val cont: (Customer) -> Shopping<A>): Shopping<A> {
        override fun <B> bind(next: (A) -> Shopping<B>): Shopping<B> =
            GetCustomer(id) { customer -> cont(customer).bind(next) }
    }
    data class Fork<R, out A>(val computation: Shopping<R>, val cont: (Shopping<R>) -> Shopping<A>): Shopping<A> {
        override fun <B> bind(next: (A) -> Shopping<B>): Shopping<B> =
            Fork(computation) { forked -> cont(forked).bind(next) }
    }
    data class Join<out A>(val thunk: () -> Any): Shopping<A> {
        override fun <B> bind(next: (A) -> Shopping<B>): Shopping<B> =
            next(thunk() as A)
    }
    data class Pure<out A>(val result: A): Shopping<A> {
        override fun <B> bind(next: (A) -> Shopping<B>): Shopping<B> = next(result)
    }

    suspend fun susp(): A = MonadDSL.susp<Shopping<A>, A>(this::bind)

    companion object {
        fun <A> shopping(block: suspend ShoppingDsl.() -> A): Shopping<A> =
            MonadDSL.effect(ShoppingDsl(), block)
    }
}

class ShoppingDsl {
    suspend fun getArticle(id: Int): Article =
        Shopping.GetArticle(id) { Shopping.Pure(it) }.susp()
    suspend fun getCustomer(id: Int): Customer =
        Shopping.GetCustomer(id) { Shopping.Pure(it) }.susp()
    suspend fun <R> fork(computation: Shopping<R>): Shopping<R> =
        Shopping.Fork(computation) { Shopping.Pure(it)}.susp()
    suspend fun <A> pure(result: A): A =
        MonadDSL.pure<Shopping<A>, A>(result) { Shopping.Pure(it) }
}

data class Future<out A>(val thunk: () -> A)

val example =
    Shopping.Fork(Shopping.GetCustomer(1) { Shopping.Pure(it) }) { c1p ->
        Shopping.Fork(Shopping.GetArticle(1) { Shopping.Pure(it) }) { a1p ->
            c1p.bind { c1 -> a1p.bind { a1 -> Shopping.Pure(c1.firstName + a1.name) }}
    } }

tailrec fun <A> shop(comp: Shopping<A>): A =
    when (comp) {
        is Shopping.GetArticle -> shop(comp.cont(Article(comp.id, "article")))
        is Shopping.Fork<*, A> ->
            shop(comp.cont(Shopping.Join( { shop(comp.computation)!! })))
        is Shopping.GetCustomer -> shop(comp.cont(Customer(comp.id.toLong(), "first", "last")))
        is Shopping.Join -> comp.thunk() as A
        is Shopping.Pure -> comp.result
    }

// FIXME: bombs with already resumed at this point

val exampleDsl = shopping {
    val c1p = fork(shopping { getCustomer(1)} )
    val a1p = fork(shopping { getArticle(1)} )
    val c1 = c1p.susp()
    val a1 = a1p.susp()
    pure(c1.firstName + a1.name)
}


class ShoppingTest {
    @Test fun example() {
        assertEquals("firstarticle", shop(example))
    }
    @Test fun exampleDsl() {
        assertEquals("firstarticle", shop(exampleDsl))
    }
}