package de.activegroup

import de.activegroup.Reader.*
import de.activegroup.Reader.Companion.reader
import de.activegroup.Reader.Companion.run
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderTest {
    @Test fun readerRun() {
        val rr1 = Ask<Int, String> { r ->
            Pure((r + 1).toString())
        }
        assertEquals(run(rr1, 7), "8")
        val rr2 = reader<Int, String> {
            val r = ask()
            pure((r + 1).toString())
        }
        assertEquals(run(rr2, 7), "8")
    }
}