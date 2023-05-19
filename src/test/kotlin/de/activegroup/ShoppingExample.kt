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
    data class Fork<R, out A>(val computation: Shopping<R>, val cont: (Future<R>) -> Shopping<A>): Shopping<A> {
        override fun <B> bind(next: (A) -> Shopping<B>): Shopping<B> =
            Fork(computation) { forked -> cont(forked).bind(next) }
    }
    data class Join<R, out A>(val future: Future<R>, val cont: (Any) -> Shopping<A>): Shopping<A> {
        override fun <B> bind(next: (A) -> Shopping<B>): Shopping<B> =
            Join(future) { result -> cont(result).bind(next) }
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
    suspend fun <R> fork(computation: Shopping<R>): Future<R> =
        Shopping.Fork(computation) { Shopping.Pure(it)}.susp()
    @Suppress("UNCHECKED_CAST")
    suspend fun <R> join(future: Future<R>): R =
        Shopping.Join(future) { Shopping.Pure(it as R)}.susp()
    suspend fun <A> pure(result: A): A =
        MonadDSL.pure<Shopping<A>, A>(result) { Shopping.Pure(it) }
}

data class Future<out A>(val thunk: () -> Any)

val example =
    Shopping.Fork(Shopping.GetCustomer(1) { Shopping.Pure(it) }) { c1p ->
        Shopping.Fork(Shopping.GetArticle(1) { Shopping.Pure(it) }) { a1p ->
            Shopping.Join(c1p) { c1 -> Shopping.Join(a1p) { a1 -> Shopping.Pure((c1 as Customer).firstName + (a1 as Article).name) }}}}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun <A> shop(comp: Shopping<A>): A =
    when (comp) {
        is Shopping.GetArticle -> shop(comp.cont(Article(comp.id, "article")))
        is Shopping.Fork<*, A> ->
            shop(comp.cont(Future { shop(comp.computation)!! }))
        is Shopping.GetCustomer -> shop(comp.cont(Customer(comp.id.toLong(), "first", "last")))
        is Shopping.Join<*, A> -> shop(comp.cont(comp.future.thunk()))
        is Shopping.Pure -> comp.result
    }

val exampleDsl = shopping {
    val customerF = fork(shopping { pure(getCustomer(1)) } )
    val articleF = fork(shopping { pure(getArticle(1)) } )
    val customer = join(customerF)
    val article = join(articleF)
    pure(customer.firstName + article.name)
}


class ShoppingTest {
    @Test fun example() {
        assertEquals("firstarticle", shop(example))
    }
    @Test fun exampleDsl() {
        assertEquals("firstarticle", shop(exampleDsl))
    }
}