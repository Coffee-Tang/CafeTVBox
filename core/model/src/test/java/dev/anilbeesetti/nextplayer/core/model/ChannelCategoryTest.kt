package dev.anilbeesetti.nextplayer.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelCategoryTest {

    @Test
    fun `however a playlist writes a cctv channel it is one of theirs`() {
        val written = listOf(
            "CCTV1",
            "CCTV-1综合",
            "CCTV-1 (1080p)",
            "cctv 1",
            "CCTV-5+体育赛事",
            "CCTV-4K",
            "CCTV建设者",
        )

        assertEquals(
            listOf(ChannelCategory.CCTV),
            written.map(::channelCategory).distinct(),
        )
    }

    @Test
    fun `a name that merely mentions cctv is not a cctv channel`() {
        assertEquals(ChannelCategory.OTHER, channelCategory("Best of CCTV"))
        assertEquals(ChannelCategory.OTHER, channelCategory("中国 CCTV 精选"))
    }

    @Test
    fun `a provincial broadcaster's national channel is a satellite channel`() {
        assertEquals(ChannelCategory.SATELLITE, channelCategory("湖南卫视"))
        assertEquals(ChannelCategory.SATELLITE, channelCategory("东方卫视4K"))
        assertEquals(ChannelCategory.SATELLITE, channelCategory("Dragon TV 东方卫视 (1080p)"))
    }

    @Test
    fun `everything else is left in the remainder rather than guessed at`() {
        assertEquals(ChannelCategory.OTHER, channelCategory("北京影视"))
        assertEquals(ChannelCategory.OTHER, channelCategory("凤凰中文"))
        assertEquals(ChannelCategory.OTHER, channelCategory("Hunan TV"))
        assertEquals(ChannelCategory.OTHER, channelCategory(""))
    }
}
