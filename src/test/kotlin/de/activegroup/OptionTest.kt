package de.activegroup

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class OptionTest {
    @Test fun optionDsl() {
        OptionDSL().effect {
            val o1 = pure(5)
            val o2 = pure(7)
            pure(o1 + o2)
        }.also { assertEquals(it, Some(12)) }
    }
}