package dev.anilbeesetti.nextplayer.feature.live.screens.channels

import dev.anilbeesetti.nextplayer.core.data.live.LiveChannels
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.model.LiveSource
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveChannelsUiStateTest {

    @Test
    fun `what the sources gave replaces what was shown`() {
        val shown = showing(channel("CCTV1"))

        val state = shown.afterReading(read(channel("CCTV1"), channel("湖南卫视")))

        assertEquals(listOf("CCTV1", "湖南卫视"), state.everyChannel.map { it.name })
        assertFalse(state.isLoading)
        assertFalse(state.loadFailed)
    }

    @Test
    fun `channels kept from an earlier run stay when no source could be read`() {
        val shown = showing(channel("CCTV1"), channel("湖南卫视"))

        val state = shown.afterReading(
            LiveChannels(channels = emptyList(), failedSources = sources(2), sourceCount = 2),
        )

        assertEquals(listOf("CCTV1", "湖南卫视"), state.everyChannel.map { it.name })
        assertFalse("a list worth showing is not a failed load", state.loadFailed)
        assertEquals(2, state.failedSourceCount)
        assertFalse(state.isLoading)
    }

    @Test
    fun `nothing read with nothing kept is a failed load`() {
        val state = LiveChannelsUiState().afterReading(
            LiveChannels(channels = emptyList(), failedSources = sources(1), sourceCount = 1),
        )

        assertTrue(state.loadFailed)
        assertTrue(state.groups.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `having no source at all is not a failed load`() {
        val state = LiveChannelsUiState().afterReading(
            LiveChannels(channels = emptyList(), failedSources = emptyList(), sourceCount = 0),
        )

        assertFalse(state.loadFailed)
        assertFalse(state.hasSources)
    }

    @Test
    fun `a group the reader picked survives a read that still offers it`() {
        val shown = showing(channel("CCTV1"), channel("湖南卫视"), channel("凤凰中文"))
            .copy(selectedGroupIndex = 2)

        val state = shown.afterReading(
            read(channel("CCTV1"), channel("湖南卫视"), channel("凤凰中文")),
        )

        assertEquals(4, state.groups.size)
        assertEquals(2, state.selectedGroupIndex)
    }

    @Test
    fun `a picked group the new list has no room for falls back to the first`() {
        val shown = showing(channel("CCTV1"), channel("湖南卫视"), channel("凤凰中文"))
            .copy(selectedGroupIndex = 3)

        val state = shown.afterReading(read(channel("CCTV1")))

        assertEquals(2, state.groups.size)
        assertEquals(0, state.selectedGroupIndex)
    }

    @Test
    fun `what one source gave replaces what was shown, under its name`() {
        val shown = LiveChannelsUiState(groups = playlistGroups(listOf(channel("CCTV1"))))

        val state = shown.afterReading(
            source = sources(1).first(),
            read = Result.success(listOf(channel("CCTV1"), channel("CCTV5"))),
        )

        assertEquals("Source 1", state.sourceName)
        assertEquals(listOf("CCTV1", "CCTV5"), state.groups.flatMap { it.channels }.map { it.name })
        assertFalse(state.loadFailed)
        assertEquals(0, state.failedSourceCount)
    }

    @Test
    fun `channels kept from an earlier run stay when the one source cannot be read`() {
        val shown = LiveChannelsUiState(groups = playlistGroups(listOf(channel("CCTV1"))))

        val state = shown.afterReading(
            source = sources(1).first(),
            read = Result.failure(IOException("no route to host")),
        )

        assertEquals(listOf("CCTV1"), state.groups.flatMap { it.channels }.map { it.name })
        assertFalse("a list worth showing is not a failed load", state.loadFailed)
        assertEquals(1, state.failedSourceCount)
    }

    @Test
    fun `a source that cannot be read with nothing kept is a failed load`() {
        val state = LiveChannelsUiState().afterReading(
            source = sources(1).first(),
            read = Result.failure(IOException("no route to host")),
        )

        assertTrue(state.loadFailed)
        assertEquals("no route to host", state.errorMessage)
        assertEquals(0, state.failedSourceCount)
    }

    /** The channels of the leading row, which lists every channel whatever its kind. */
    private val LiveChannelsUiState.everyChannel: List<LiveChannel>
        get() = groups.first().channels

    private fun showing(vararg channels: LiveChannel) =
        LiveChannelsUiState(groups = categoryGroups(channels.toList()), isLoading = false)

    private fun read(vararg channels: LiveChannel) = LiveChannels(
        channels = channels.toList(),
        failedSources = emptyList(),
        sourceCount = 1,
    )

    private fun sources(count: Int) = (1..count).map { index ->
        LiveSource(id = index.toLong(), name = "Source $index", url = "https://$index/tv.m3u")
    }

    private fun channel(name: String) = LiveChannel(name = name, urls = listOf("http://stream/$name"))
}
