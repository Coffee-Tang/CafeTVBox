package dev.anilbeesetti.nextplayer.core.data.live

import dev.anilbeesetti.nextplayer.core.model.LiveChannel

/**
 * Parses the text of an m3u / m3u8 playlist into [LiveChannel]s.
 *
 * Supports the common IPTV extensions: `#EXTINF` with `tvg-id`, `tvg-name`, `tvg-logo` and
 * `group-title` attributes, plus `#EXTGRP` as a fallback group. Attribute values may contain
 * commas because the channel title is taken at the first comma that sits outside quotes.
 */
object M3uParser {

    private val attributeRegex = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(content: String): List<LiveChannel> {
        val channels = mutableListOf<LiveChannel>()

        var pendingName: String? = null
        var pendingGroup = ""
        var pendingLogo: String? = null
        var pendingTvgId: String? = null
        var pendingTvgName: String? = null
        var extGroup = ""

        fun reset() {
            pendingName = null
            pendingGroup = ""
            pendingLogo = null
            pendingTvgId = null
            pendingTvgName = null
            extGroup = ""
        }

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim().removePrefix("\uFEFF").trim()
            when {
                line.isEmpty() -> continue

                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val info = line.substringAfter(":", "").let { it }
                    val attributes = attributeRegex.findAll(info)
                        .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    pendingLogo = attributes["tvg-logo"]?.takeIf { it.isNotBlank() }
                    pendingTvgId = attributes["tvg-id"]?.takeIf { it.isNotBlank() }
                    pendingTvgName = attributes["tvg-name"]?.takeIf { it.isNotBlank() }
                    pendingGroup = attributes["group-title"].orEmpty()
                    pendingName = extractTitle(info)
                }

                line.startsWith("#EXTGRP", ignoreCase = true) -> {
                    extGroup = line.substringAfter(":", "").trim()
                }

                line.startsWith("#") -> continue

                else -> {
                    val name = pendingName?.takeIf { it.isNotBlank() }
                        ?: pendingTvgName?.takeIf { it.isNotBlank() }
                        ?: line.substringAfterLast('/').ifBlank { line }
                    val group = pendingGroup.takeIf { it.isNotBlank() } ?: extGroup
                    channels += LiveChannel(
                        name = name,
                        url = line,
                        group = group,
                        logoUrl = pendingLogo,
                        tvgId = pendingTvgId,
                    )
                    reset()
                }
            }
        }
        return channels
    }

    /**
     * Returns the channel title: everything after the first comma that is not inside quotes.
     */
    private fun extractTitle(info: String): String {
        var inQuotes = false
        for (index in info.indices) {
            when (info[index]) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) return info.substring(index + 1).trim()
            }
        }
        return ""
    }
}
