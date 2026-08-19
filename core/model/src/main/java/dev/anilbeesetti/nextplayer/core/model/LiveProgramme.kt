package dev.anilbeesetti.nextplayer.core.model

/**
 * One entry of a channel's schedule, as listed by an XMLTV guide.
 *
 * [channelName] is the name the guide files the entry under, which is matched against
 * [LiveChannel.name]. [start] and [end] are epoch milliseconds.
 */
data class LiveProgramme(
    val channelName: String,
    val title: String,
    val start: Long,
    val end: Long,
) {

    fun isOnAt(timeMillis: Long): Boolean = timeMillis >= start && timeMillis < end
}
