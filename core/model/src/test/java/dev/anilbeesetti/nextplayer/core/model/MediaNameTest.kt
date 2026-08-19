package dev.anilbeesetti.nextplayer.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaNameTest {

    @Test
    fun `a release name carrying both titles gives up all of them`() {
        assertEquals(
            MediaName.Episode(
                title = "Silicon Valley",
                otherTitle = "硅谷",
                season = 3,
                episode = 1,
                seasonYear = 2016,
            ),
            mediaName(
                fileName = "硅谷.Silicon.Valley.2016.S03E01.Founder.Friendly.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv",
                folderName = "SE03（10集）1080P",
            ),
        )
    }

    @Test
    fun `the subtitles beside a video read the same as the video`() {
        val video = "硅谷.Silicon.Valley.2015.S02E01.Sand.Hill.Shuffle.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv"
        val subtitles = "硅谷.Silicon.Valley.2015.S02E01.Sand.Hill.Shuffle.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.ChsEngA.ass"

        assertEquals(
            mediaName(video, folderName = "SE02（8集）1080P"),
            mediaName(subtitles, folderName = "SE02（8集）1080P"),
        )
    }

    @Test
    fun `a release named without an episode title is read as far as it goes`() {
        assertEquals(
            MediaName.Episode("Silicon Valley", "硅谷", season = 6, episode = 1, seasonYear = 2019),
            mediaName("硅谷.Silicon.Valley.2019.S06E01.proper.1080p.web.h264-tbs.mkv", "SE06（7集）1080P"),
        )
        assertEquals(
            MediaName.Episode("Silicon Valley", "硅谷", season = 6, episode = 5, seasonYear = 2019),
            mediaName("硅谷.Silicon.Valley.2019.S06E05.1080p.AMZN.WEB-DL.DDP5.1.H.264-NTb.mkv", "SE06（7集）1080P"),
        )
        assertEquals(
            MediaName.Episode("Silicon Valley", "硅谷", season = 6, episode = 2, seasonYear = 2019),
            mediaName("硅谷.Silicon.Valley.2019.S06E02.repack.1080p.web.h264-tbs.简体&英文.ass", "SE06（7集）1080P"),
        )
    }

    @Test
    fun `the year an episode carries dates its season and not its series`() {
        val third = mediaName(
            "硅谷.Silicon.Valley.2016.S03E01.Founder.Friendly.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv",
            "SE03（10集）1080P",
        ) as MediaName.Episode
        val sixth = mediaName(
            "硅谷.Silicon.Valley.2019.S06E01.proper.1080p.web.h264-tbs.mkv",
            "SE06（7集）1080P",
        ) as MediaName.Episode

        assertEquals(third.title, sixth.title)
        assertEquals(2016, third.seasonYear)
        assertEquals(2019, sixth.seasonYear)
    }

    @Test
    fun `a file that only numbers its episode is named by the folder holding it`() {
        assertEquals(
            MediaName.Episode(
                title = "天道",
                otherTitle = null,
                season = 1,
                episode = 1,
                seasonYear = null,
            ),
            mediaName("[4K超高清修复]无删减完整版第01集_超清 4K.mp4", "天道【4K超高清修复】"),
        )
    }

    @Test
    fun `a folder that names no work is refused rather than taken for a title`() {
        assertNull(mediaName("[4K超高清修复]无删减完整版第01集_超清 4K.mp4", "SE03（10集）1080P"))
        assertNull(mediaName("第01集.mp4", "SE01（8集）1080P"))
    }

    @Test
    fun `the episodes a season folder counts are not an episode number`() {
        assertEquals(
            MediaName.Film(title = "Silicon Valley", otherTitle = "硅谷", year = 2016),
            mediaName("硅谷.Silicon.Valley.2016.1080p.AMZN.WEB-DL.mkv", "SE03（10集）1080P"),
        )
    }

    @Test
    fun `a work keeps a name that a promise of picture quality is wrapped around`() {
        assertEquals(
            MediaName.Episode("天道", null, season = 1, episode = 24, seasonYear = null),
            mediaName("[4K超高清修复]无删减完整版第24集_超清 4K.mp4", "天道【4K超高清修复】"),
        )
    }

    @Test
    fun `a film is read with its year and with neither season nor episode`() {
        assertEquals(
            MediaName.Film(title = "Inception", otherTitle = "盗梦空间", year = 2010),
            mediaName("盗梦空间.Inception.2010.1080p.BluRay.x264-AMIABLE.mkv", "电影"),
        )
    }

    @Test
    fun `a film named after a year keeps the year as its name`() {
        assertEquals(
            MediaName.Film(title = "2012", otherTitle = null, year = 2009),
            mediaName("2012.2009.1080p.BluRay.x264.mkv", "电影"),
        )
    }

    @Test
    fun `a name with nothing in it names nothing`() {
        assertNull(mediaName("", ""))
        assertNull(mediaName("1080p.WEB-DL.mkv", "SE05（8集）1080P"))
    }
}
