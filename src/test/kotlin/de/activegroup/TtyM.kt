package de.activegroup

import de.activegroup.TtyM.Companion.output
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals

class TtyMTest {

    @Test fun ttyMOutput() {
        val tty = TtyDsl().effect {
            write("foo")
            write("bar")
            pure(5)
        }
        val output = mutableListOf<String>()
        runBlocking { output(tty, output) }
        assertContentEquals(output.asSequence(), sequenceOf("foo", "bar"))
    }
}