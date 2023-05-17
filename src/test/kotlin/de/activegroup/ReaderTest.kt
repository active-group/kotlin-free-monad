package de.activegroup

import de.activegroup.Reader.Companion.reader
import de.activegroup.Reader.Companion.run
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderTest {
    @Test fun readerRun() {
        val rr = reader<Int, Int> {
            val r: Int = ask()
            pure(r+1)
        }
        assertEquals(rr.run(7), 8)

    }
}