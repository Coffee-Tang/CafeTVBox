package dev.anilbeesetti.nextplayer.core.model

/**
 * Reduces a channel name to a key that identifies the station wherever it is named.
 *
 * Every playlist and every programme guide writes the same station its own way: `CCTV1`,
 * `CCTV-1综合`, `CCTV-1 (1080p)`, `东方卫视4K`. Matching names as written therefore fails between a
 * playlist and a guide, and between two playlists offering the same station. The key exists only
 * for matching; what the user reads stays the name their playlist gave.
 *
 * Stations that are named in different languages, such as `广东卫视` and `Guangdong Satellite TV`,
 * are beyond what folding punctuation can do and stay separate.
 */
fun channelKey(name: String): String {
    val withoutAsides = bracketedAside.replace(name, "")
    val bare = separators.replace(withoutAsides.uppercase(), "")
    val undecorated = bare.withoutTrailingDecorations()
    val cctv = cctvChannel.find(undecorated) ?: return undecorated
    return "CCTV${cctv.groupValues[1]}${cctv.groupValues[2]}"
}

/** Notes such as `(1080p)`, `[Not 24/7]` or `【高清】` describe the stream, not the station. */
private val bracketedAside = Regex("""[(\[（【][^)\]）】]*[)\]）】]""")

private val separators = Regex("""[\s\-_·•,.:;'"/\\|、，。：；－]+""")

/**
 * `CCTV-1综合`, `CCTV1` and `CCTV-1` are one station, so only the number is kept. The trailing plus
 * is part of the number: CCTV5 and CCTV5+ are two stations. `CCTV+1` is a different family again
 * and falls outside this pattern, which is why the number has to come first. `CCTV-4K` is a station
 * of its own rather than a fourth channel, hence the K it refuses to end on.
 */
private val cctvChannel = Regex("""^CCTV(\d{1,2})(\+?)(?!K)""")

/** Wording no station is named after, so it can only describe the picture. */
private val qualityWords = listOf("高清", "超清", "标清", "FHD", "UHD", "HD", "IPV4", "IPV6")

/**
 * `4K` and `8K` describe the picture in `东方卫视4K`, but name the station in `CCTV-4K`. Only a
 * Chinese name in front of them settles which, as `CCTV-8K` shows they cannot simply be dropped.
 */
private val trailingResolution = Regex("""(\p{IsHan})(4K|8K)$""")

private fun String.withoutTrailingDecorations(): String {
    var name = this
    while (true) {
        val quality = qualityWords.firstOrNull { name.length > it.length && name.endsWith(it) }
        name = when {
            quality != null -> name.removeSuffix(quality)
            trailingResolution.containsMatchIn(name) -> name.dropLast(2)
            else -> return name
        }
    }
}
