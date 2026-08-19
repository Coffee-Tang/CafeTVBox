package dev.anilbeesetti.nextplayer.feature.network.screens.browse

import dev.anilbeesetti.nextplayer.core.model.NetworkFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderQueueTest {

    private val listing = listOf(
        directory("Extras"),
        file("Episode.01.mkv"),
        file("Episode.02.mkv"),
        file("Episode.03.mkv"),
    )

    @Test
    fun `queue keeps the listed order and starts at the chosen file`() {
        val queue = folderQueueFor(files = listing, clicked = file("Episode.02.mkv"))

        assertEquals(
            listOf("Episode.01.mkv", "Episode.02.mkv", "Episode.03.mkv"),
            queue?.files?.map { it.name },
        )
        assertEquals(1, queue?.startIndex)
    }

    @Test
    fun `folders are left out of the queue`() {
        val queue = folderQueueFor(files = listing, clicked = file("Episode.01.mkv"))

        assertEquals(0, queue?.startIndex)
        assertEquals(emptyList<String>(), queue?.files?.filter { it.isDirectory }?.map { it.name })
    }

    @Test
    fun `a folder cannot be queued`() {
        assertNull(folderQueueFor(files = listing, clicked = directory("Extras")))
    }

    @Test
    fun `a file from another listing cannot be queued`() {
        assertNull(folderQueueFor(files = listing, clicked = file("Elsewhere.mkv")))
    }

    private fun file(name: String) = NetworkFile(name = name, path = "/Shows/$name", isDirectory = false)

    private fun directory(name: String) = NetworkFile(name = name, path = "/Shows/$name", isDirectory = true)
}
