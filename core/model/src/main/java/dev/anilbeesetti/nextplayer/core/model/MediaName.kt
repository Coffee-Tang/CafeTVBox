package dev.anilbeesetti.nextplayer.core.model

/**
 * What a file's name says about the work it holds, in the form a catalogue lookup needs.
 *
 * A film and an episode are told apart here rather than left to the reader, because the year in a
 * name means a different thing to each of them and only one of the two can be trusted to narrow a
 * search; see [Film.year] and [Episode.seasonYear].
 */
sealed interface MediaName {

    /**
     * The title to look the work up by.
     *
     * A name carrying two languages, such as `硅谷.Silicon.Valley.2016.S03E01`, writes the Chinese
     * title first, but the Latin one leads here: a catalogue indexes it whether or not anyone has
     * filed a translation of it, so it is the one that always finds the work.
     */
    val title: String

    /**
     * The work's title in the other language the name carries, and null when it carries only one.
     * It names the same work as [title] and is worth searching with too.
     */
    val otherTitle: String?

    /** A work whose name shows neither a season nor an episode. */
    data class Film(
        override val title: String,
        override val otherTitle: String?,
        /**
         * The year the film came out, when the name gives one. A film is released once, so this
         * dates the work itself and is firm enough to narrow a search by.
         */
        val year: Int?,
    ) : MediaName

    /**
     * One episode of a series. A name that numbers episodes without naming a season, as
     * `第01集` does, is read as the first season, which is what a library written that way means.
     */
    data class Episode(
        override val title: String,
        override val otherTitle: String?,
        val season: Int,
        val episode: Int,
        /**
         * The year an episode's name gives, which dates the season and not the series:
         * `硅谷.Silicon.Valley.2016.S03E01` says 2016 because that is when the third season aired,
         * while the catalogue dates the series from 2014, when it first did. So this may only rank
         * candidates a title search has already found; filtering by it throws away the very series
         * whose file it was read from.
         */
        val seasonYear: Int?,
    ) : MediaName
}

/**
 * Reads what [fileName] and the [folderName] directly above it say about the work, or null when
 * neither of them names one.
 *
 * The file name is read first and the folder answers only for what it left out. `SE03（10集）1080P`
 * holds `硅谷.Silicon.Valley.2016.S03E01.Founder.Friendly.1080p.AMZN.WEB-DL...`, where the file
 * says everything and the folder nothing, while `天道【4K超高清修复】` holds
 * `[4K超高清修复]无删减完整版第01集_超清 4K`, where the file is decoration around an episode number and
 * only the folder names the work. A folder is no more bound to name a work than a file is, so
 * `SE03（10集）1080P` cleans away to nothing and is refused rather than becoming a title.
 *
 * Whether [fileName] is media at all is the caller's question, not this one's: a video and the
 * subtitles beside it are given the same name (`...SiGMA.mkv`, `...SiGMA.ChsEngA.ass`) and read
 * the same here, while the scanner that walks a library already tells them apart by extension.
 * A second list of extensions kept here would only be something for that one to drift from.
 */
fun mediaName(fileName: String, folderName: String): MediaName? {
    val file = read(fileName.substringBeforeLast('.'))
    val folder = read(folderName)
    val titles = file.titles ?: folder.titles ?: return null
    val year = file.year ?: folder.year
    val episode = file.episode ?: return MediaName.Film(titles.title, titles.otherTitle, year)
    return MediaName.Episode(
        title = titles.title,
        otherTitle = titles.otherTitle,
        season = file.season ?: 1,
        episode = episode,
        seasonYear = year,
    )
}

private class ReadName(val titles: Titles?, val year: Int?, val season: Int?, val episode: Int?)

private class Titles(val title: String, val otherTitle: String?)

/**
 * A name is read from the left: it names the work until it starts numbering or dating it, and
 * everything from there on describes the release instead.
 */
private fun read(name: String): ReadName {
    val numbered = seasonAndEpisode.find(name)
    val counted = chineseEpisode.find(name)
    val dated = standaloneYear.findAll(name).firstOrNull { titlesIn(name.take(it.range.first)) != null }
    val titleEnds = listOfNotNull(numbered, counted, dated).minOfOrNull { it.range.first } ?: name.length
    return ReadName(
        titles = titlesIn(name.take(titleEnds)),
        year = dated?.value?.toInt(),
        season = numbered?.groupValues?.get(1)?.toInt(),
        episode = numbered?.groupValues?.get(2)?.toInt() ?: counted?.groupValues?.get(1)?.toInt(),
    )
}

/**
 * The titles left in [zone] once everything that describes the release rather than the work is
 * taken out of it, or null when nothing is left, as in `SE03（10集）1080P` or `无删减完整版`.
 */
private fun titlesIn(zone: String): Titles? {
    val words = bracketedAside.replace(zone, " ")
        .split(separators)
        .map { it.trim { character -> character in brackets } }
        .filter { it.any(Char::isLetterOrDigit) }
        .takeWhile { !it.describesTheRelease() }
    val chinese = words.filter(hanScript::containsMatchIn).joinToString("").withoutChineseDecorations()
    val latin = words.filterNot(hanScript::containsMatchIn).joinToString(" ")
    return when {
        latin.isNotEmpty() -> Titles(latin, chinese.ifEmpty { null })
        chinese.isNotEmpty() -> Titles(chinese, null)
        else -> null
    }
}

/** `S03E01`, whose season the folder above it usually repeats and whose episode nothing else gives. */
private val seasonAndEpisode = Regex("""(?<![A-Za-z0-9])S(\d{1,2})E(\d{1,3})(?!\d)""", RegexOption.IGNORE_CASE)

/**
 * `第01集`, the Chinese way of numbering an episode. The `第` is what makes it one: `（10集）` in a
 * folder name counts the season's episodes instead, and reading it as an episode number would turn
 * every such folder into the tenth episode of something.
 */
private val chineseEpisode = Regex("""第(\d{1,3})集""")

/**
 * A four-digit year standing on its own, so that `2160p` stays a resolution. A year is only taken
 * where something is named in front of it, which leaves the films titled `2012` and `1917` their
 * names.
 */
private val standaloneYear = Regex("""(?<![A-Za-z0-9])(?:19|20)\d{2}(?![A-Za-z0-9])""")

/** Notes such as `[4K超高清修复]`, `（10集）` or `(2016)` describe the release, not the work. */
private val bracketedAside = Regex("""[(\[（【][^)\]）】]*[)\]）】]""")

private val separators = Regex("""[\s._·•、,，+&/\\|]+""")

private const val brackets = "[]()（）【】《》"

private val hanScript = Regex("""\p{IsHan}""")

/** `1080p`, `4K` and the `SE03` a season folder is prefixed with. */
private val releaseShorthand = Regex("""\d{3,4}[pi]|[48]k|se\d{1,2}""", RegexOption.IGNORE_CASE)

/** Where a name stops naming the work and starts describing the file it was packed into. */
private val releaseWords = setOf(
    "WEB", "WEB-DL", "WEBDL", "WEBRIP", "BLURAY", "BDRIP", "DVDRIP", "HDTV", "REMUX",
    "AMZN", "PROPER", "REPACK", "X264", "X265", "H264", "H265", "HEVC", "AVC",
    "AAC", "AC3", "DTS", "DD", "DDP", "UHD", "HDR", "CHSENGA",
)

private fun String.describesTheRelease(): Boolean =
    uppercase() in releaseWords || releaseShorthand.matches(this)

/**
 * Wording that promises a better picture or a fuller cut than the last upload, which no work is
 * named after. `无删减完整版` is made only of it and leaves nothing behind, while `天道` keeps its
 * name; taking these out of the middle of a title would be what breaks that, so they only go from
 * its ends.
 */
private val chineseDecorations = listOf(
    "无删减", "未删减", "完整版", "加长版", "修复版", "修复", "重制",
    "超高清", "超清", "高清", "标清", "蓝光", "全集", "中字", "国语", "简体", "繁体", "英文",
)

private fun String.withoutChineseDecorations(): String {
    var name = this
    while (true) {
        val decoration = chineseDecorations.firstOrNull { name.startsWith(it) || name.endsWith(it) }
            ?: return name
        name = name.removePrefix(decoration).removeSuffix(decoration)
    }
}
