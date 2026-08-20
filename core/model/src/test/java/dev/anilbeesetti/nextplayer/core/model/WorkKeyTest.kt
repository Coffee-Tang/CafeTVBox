package dev.anilbeesetti.nextplayer.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WorkKeyTest {

    @Test
    fun `the ways a file writes one title share a key`() {
        val written = listOf(
            "Silicon Valley",
            "Silicon.Valley",
            "silicon valley",
            "SILICON_VALLEY",
        )

        assertEquals(setOf("siliconvalley"), written.map(::workKey).toSet())
    }

    @Test
    fun `a chinese title is left as itself`() {
        assertEquals("硅谷", workKey("硅谷"))
        assertEquals("天道", workKey("天道"))
    }

    @Test
    fun `titles that only look alike stay apart`() {
        assertNotEquals(workKey("天道"), workKey("大道通天"))
        assertNotEquals(workKey("Silicon Valley"), workKey("Silicon Valley The Untold Story"))
    }
}
