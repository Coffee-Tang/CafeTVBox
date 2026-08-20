package dev.anilbeesetti.nextplayer.core.model.search

import org.junit.Assert.assertEquals
import org.junit.Test

class RankingTest {

    private val works = listOf("Silicon Valley", "Death in the Valley", "天道", "硅谷")

    @Test
    fun `only what the query names is kept`() {
        assertEquals(
            listOf("Silicon Valley", "Death in the Valley"),
            works.rankedBy("valley") { listOf(it) },
        )
    }

    @Test
    fun `the nearer answer comes first`() {
        assertEquals(
            listOf("天道", "硅谷"),
            listOf("硅谷", "天道").rankedBy("天道") { listOf(it, "硅谷 天道") },
        )
    }

    @Test
    fun `a query naming nothing leaves nothing`() {
        assertEquals(emptyList<String>(), works.rankedBy("westworld") { listOf(it) })
    }

    @Test
    fun `an empty query is not taken to name everything`() {
        assertEquals(emptyList<String>(), works.rankedBy("") { listOf(it) })
    }

    @Test
    fun `items that answer alike keep the order they came in`() {
        val ranked = listOf("Valley A", "Valley B", "Valley C").rankedBy("valley") { listOf(it) }

        assertEquals(listOf("Valley A", "Valley B", "Valley C"), ranked)
    }

    @Test
    fun `any of the names an item goes by can answer for it`() {
        val ranked = works.rankedBy("silicon") { title ->
            listOf(title, if (title == "硅谷") "Silicon Valley" else null)
        }

        assertEquals(listOf("Silicon Valley", "硅谷"), ranked)
    }

    @Test
    fun `a chinese title is found by the initials of its pinyin`() {
        assertEquals(listOf("硅谷"), works.rankedBy("gg") { listOf(it) })
    }
}
