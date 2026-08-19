package dev.anilbeesetti.nextplayer.feature.live.screens.channels

import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveChannelGroupsTest {

    @Test
    fun `every channel is listed before the categories they fall into`() {
        val groups = categoryGroups(
            listOf(
                channel("湖南卫视", group = "卫视频道"),
                channel("CCTV-1综合", group = "央视频道"),
                channel("凤凰中文", group = "General"),
            ),
        )

        assertEquals(
            listOf(
                R.string.live_category_all,
                R.string.live_category_cctv,
                R.string.live_category_satellite,
                R.string.live_category_other,
            ),
            groups.map { it.labelRes },
        )
        assertEquals(3, groups.first().channels.size)
        assertEquals(listOf("CCTV-1综合"), groups[1].channels.map { it.name })
        assertEquals(listOf("湖南卫视"), groups[2].channels.map { it.name })
        assertEquals(listOf("凤凰中文"), groups[3].channels.map { it.name })
    }

    @Test
    fun `a category no channel belongs to is left out`() {
        val groups = categoryGroups(
            listOf(channel("CCTV-5+体育赛事"), channel("CCTV-4K")),
        )

        assertEquals(
            listOf(R.string.live_category_all, R.string.live_category_cctv),
            groups.map { it.labelRes },
        )
    }

    @Test
    fun `nothing to show is no rows at all`() {
        assertEquals(emptyList<LiveChannelGroup>(), categoryGroups(emptyList()))
    }

    @Test
    fun `one source keeps the groups its playlist wrote, in the order it wrote them`() {
        val groups = playlistGroups(
            listOf(
                channel("CCTV-1综合", group = "央视"),
                channel("湖南卫视", group = "卫视"),
                channel("CCTV-5体育", group = "央视"),
                channel("Local", group = ""),
            ),
        )

        assertEquals(listOf("央视", "卫视", ""), groups.map { it.name })
        assertEquals(listOf(null, null, null), groups.map { it.labelRes })
        assertEquals(listOf("CCTV-1综合", "CCTV-5体育"), groups.first().channels.map { it.name })
    }

    private fun channel(name: String, group: String = "") =
        LiveChannel(name = name, urls = listOf("http://example.test/$name.m3u8"), group = group)
}
