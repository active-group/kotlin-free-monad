package de.activegroup

import de.activegroup.Option.Companion.optionally
import de.activegroup.Option.Companion.some
import kotlin.test.Test
import kotlin.test.assertEquals

class OptionTest {

    @Test fun optionMonad() {
        some(5).bind { o1 ->
            some(7).bind { o2 ->
                some(o1 + o2)
            }
        }.also { assertEquals(it, Some(12)) }
    }

    @Test fun optionDsl() {
        optionally {
            val o1 = pure(5)
            val o2 = pure(7)
            pure(o1 + o2)
        }.also { assertEquals(it, Some(12)) }
    }
}