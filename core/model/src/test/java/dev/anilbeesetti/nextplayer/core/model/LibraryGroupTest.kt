package dev.anilbeesetti.nextplayer.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryGroupTest {

    @Test
    fun `files that name a work in two languages become one work`() {
        val grouped = groupLibrary(
            listOf(
                entry("硅谷.Silicon.Valley.2016.S03E01.mkv", "SE03（10集）1080P"),
                entry("[4K超高清修复]无删减完整版第01集_超清 4K.mp4", "天道【4K超高清修复】"),
                entry("第02集.mp4", "硅谷"),
            ),
        )

        assertEquals(listOf("Silicon Valley", "天道"), grouped.works.map { it.title })
        val silicon = grouped.works.single { it.title == "Silicon Valley" }
        assertEquals("siliconvalley", silicon.workKey)
        assertEquals("硅谷", silicon.otherTitle)
        assertEquals(setOf("siliconvalley", "硅谷"), silicon.aliases)
        assertEquals(listOf(1 to 2, 3 to 1), silicon.items.map { it.season to it.episode })
        assertTrue(grouped.unread.isEmpty())
    }

    @Test
    fun `two files of one episode become two files of one item`() {
        val grouped = groupLibrary(
            listOf(
                entry("硅谷.Silicon.Valley.2016.S03E01.1080p.mkv", "SE03", "s3e1-1080.mkv"),
                entry("硅谷.Silicon.Valley.2016.S03E01.2160p.mkv", "SE03", "s3e1-2160.mkv"),
            ),
        )

        val item = grouped.works.single().items.single()
        assertEquals(3, item.season)
        assertEquals(1, item.episode)
        assertEquals(
            listOf("s3e1-1080.mkv", "s3e1-2160.mkv"),
            item.files.map { it.path },
        )
    }

    @Test
    fun `a film and a series of the same title stay apart`() {
        val grouped = groupLibrary(
            listOf(
                entry("Inception.2010.mkv", "Movies"),
                entry("Inception.S01E01.mkv", "Inception"),
            ),
        )

        assertEquals(
            setOf(WorkKind.FILM, WorkKind.SERIES),
            grouped.works.map { it.kind }.toSet(),
        )
        val film = grouped.works.single { it.kind == WorkKind.FILM }
        assertEquals(GroupedItem.FILM_SEASON, film.items.single().season)
        assertEquals(GroupedItem.FILM_EPISODE, film.items.single().episode)
        assertEquals(2010, film.year)
        val series = grouped.works.single { it.kind == WorkKind.SERIES }
        assertEquals(1 to 1, series.items.single().season to series.items.single().episode)
        assertEquals(null, series.year)
    }

    @Test
    fun `a series library refuses a film`() {
        val film = entry("Inception.2010.mkv", "Movies")
        val grouped = groupLibrary(
            listOf(film, entry("硅谷.Silicon.Valley.2016.S03E01.mkv", "SE03")),
            kind = WorkKind.SERIES,
        )

        assertEquals(listOf("Silicon Valley"), grouped.works.map { it.title })
        assertEquals(listOf(film), grouped.unread)
    }

    @Test
    fun `a file that does not name a work is left unread`() {
        val nameless = entry("1080p.mkv", "SE03（10集）1080P")
        val grouped = groupLibrary(listOf(nameless))

        assertTrue(grouped.works.isEmpty())
        assertEquals(listOf(nameless), grouped.unread)
    }

    @Test
    fun `a poster named as a work is a film unless the caller has already set pictures aside`() {
        val poster = entry("海报.jpg", "天道【4K超高清修复】")
        val grouped = groupLibrary(listOf(poster))

        val film = grouped.works.single()
        assertEquals(WorkKind.FILM, film.kind)
        assertEquals("海报", film.title)
        assertTrue(grouped.unread.isEmpty())
    }

    @Test
    fun `the real share is two series and not a folder each`() {
        val grouped = groupLibrary(theShare, kind = WorkKind.SERIES)

        assertEquals(listOf("Silicon Valley", "天道"), grouped.works.map { it.title })
        assertTrue(grouped.unread.isEmpty())

        val silicon = grouped.works.single { it.workKey == "siliconvalley" }
        assertEquals("硅谷", silicon.otherTitle)
        assertEquals(
            listOf(
                2 to (1..10).toList(),
                3 to (1..10).toList(),
                4 to (1..10).toList(),
                5 to listOf(1, 2, 3, 4, 5, 7, 8),
                6 to (1..7).toList(),
            ),
            silicon.items.groupBy { it.season }.toSortedMap().map { (season, items) ->
                season to items.map { it.episode }
            },
        )
        assertEquals(44, silicon.items.sumOf { it.files.size })

        val tiandao = grouped.works.single { it.workKey == "天道" }
        assertEquals(null, tiandao.otherTitle)
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6, 8, 9) + (11..24),
            tiandao.items.map { it.episode },
        )
        assertEquals(22, tiandao.items.size)
        assertTrue(tiandao.items.none { it.episode == 7 || it.episode == 10 })
    }
}

private fun entry(fileName: String, folderName: String, path: String = fileName) =
    LibraryEntry(fileName = fileName, folderName = folderName, path = path)

/**
 * The videos on the share this library was designed against, as of the scan that locked the
 * grouping: six season folders of Silicon Valley (SE01 empty, SE05 missing episode 6) and 天道
 * missing 07 and 10. The folder names count episodes and are wrong for SE02, which is why grouping
 * must not trust them.
 */
private val theShare = """
    SE02（8集）1080P/硅谷.Silicon.Valley.2015.S02E01.Sand.Hill.Shuffle.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE02（8集）1080P/硅谷.Silicon.Valley.2015.S02E02.Runaway.Devaluation.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE02（8集）1080P/硅谷.Silicon.Valley.2015.S02E03.Bad.Money.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE02（8集）1080P/硅谷.Silicon.Valley.2015.S02E04.The.Lady.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE02（8集）1080P/硅谷.Silicon.Valley.2015.S02E05.Server.Space.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE02（8集）1080P/硅谷.Silicon.Valley.2015.S02E06.Homicide.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE02（8集）1080P/硅谷.Silicon.Valley.2015.S02E07.Adult.Content.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE02（8集）1080P/硅谷.Silicon.Valley.2015.S02E08.White.HatBlack.Hat.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE02（8集）1080P/硅谷.Silicon.Valley.2015.S02E09.Binding.Arbitration.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE02（8集）1080P/硅谷.Silicon.Valley.2015.S02E10.Two.Days.of.the.Condor.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE03（10集）1080P/硅谷.Silicon.Valley.2016.S03E01.Founder.Friendly.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE03（10集）1080P/硅谷.Silicon.Valley.2016.S03E02.Two.in.the.Box.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE03（10集）1080P/硅谷.Silicon.Valley.2016.S03E03.Meinertzhagens.Haversack.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE03（10集）1080P/硅谷.Silicon.Valley.2016.S03E04.Maleant.Data.Systems.Solutions.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE03（10集）1080P/硅谷.Silicon.Valley.2016.S03E05.The.Empty.Chair.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE03（10集）1080P/硅谷.Silicon.Valley.2016.S03E06.Bachmanity.Insanity.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE03（10集）1080P/硅谷.Silicon.Valley.2016.S03E07.To.Build.a.Better.Beta.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE03（10集）1080P/硅谷.Silicon.Valley.2016.S03E08.Bachmans.Earnings.Over-Ride.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE03（10集）1080P/硅谷.Silicon.Valley.2016.S03E09.Daily.Active.Users.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE03（10集）1080P/硅谷.Silicon.Valley.2016.S03E10.The.Uptick.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE04（10集）1080P/硅谷.Silicon.Valley.2017.S04E01.Success.Failure.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE04（10集）1080P/硅谷.Silicon.Valley.2017.S04E02.Terms.of.Service.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE04（10集）1080P/硅谷.Silicon.Valley.2017.S04E03.Intellectual.Property.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE04（10集）1080P/硅谷.Silicon.Valley.2017.S04E04.Teambuilding.Exercise.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE04（10集）1080P/硅谷.Silicon.Valley.2017.S04E05.The.Blood.Boy.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE04（10集）1080P/硅谷.Silicon.Valley.2017.S04E06.Customer.Service.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE04（10集）1080P/硅谷.Silicon.Valley.2017.S04E07.The.Patent.Troll.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE04（10集）1080P/硅谷.Silicon.Valley.2017.S04E08.The.Keenan.Vortex.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE04（10集）1080P/硅谷.Silicon.Valley.2017.S04E09.Hooli-Con.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE04（10集）1080P/硅谷.Silicon.Valley.2017.S04E10.Server.Error.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE05（8集）1080P/硅谷.Silicon.Valley.2018.S05E01.Grow.Fast.or.Die.Slow.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE05（8集）1080P/硅谷.Silicon.Valley.2018.S05E02.Reorientation.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE05（8集）1080P/硅谷.Silicon.Valley.2018.S05E03.Chief.Operating.Officer.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE05（8集）1080P/硅谷.Silicon.Valley.2018.S05E04.Tech.Evangelist.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE05（8集）1080P/硅谷.Silicon.Valley.2018.S05E05.Facial.Recognition.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE05（8集）1080P/硅谷.Silicon.Valley.2018.S05E07.Initial.Coin.Offering.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE05（8集）1080P/硅谷.Silicon.Valley.2018.S05E08.Fifty-One.Percent.1080p.AMZN.WEB-DL.DD.5.1.H.265-SiGMA.mkv
    SE06（7集）1080P/硅谷.Silicon.Valley.2019.S06E01.proper.1080p.web.h264-tbs.mkv
    SE06（7集）1080P/硅谷.Silicon.Valley.2019.S06E02.repack.1080p.web.h264-tbs.mkv
    SE06（7集）1080P/硅谷.Silicon.Valley.2019.S06E03.1080p.web.h264-tbs.mkv
    SE06（7集）1080P/硅谷.Silicon.Valley.2019.S06E04.1080p.web.h264-tbs.mkv
    SE06（7集）1080P/硅谷.Silicon.Valley.2019.S06E05.1080p.AMZN.WEB-DL.DDP5.1.H.264-NTb.mkv
    SE06（7集）1080P/硅谷.Silicon.Valley.2019.S06E06.repack.1080p.web.h264-tbs.mkv
    SE06（7集）1080P/硅谷.Silicon.Valley.2019.S06E07.1080p.web.h264-tbs.mkv
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第01集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第02集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第03集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第04集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第05集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第06集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第08集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第09集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第11集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第12集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第13集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第14集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第15集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第16集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第17集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第18集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第19集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第20集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第21集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第22集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第23集_超清 4K.mp4
    天道【4K超高清修复】/[4K超高清修复]无删减完整版第24集_超清 4K.mp4
""".trimIndent().lines().map { relative ->
    LibraryEntry(
        fileName = relative.substringAfter('/'),
        folderName = relative.substringBefore('/'),
        path = relative,
    )
}
