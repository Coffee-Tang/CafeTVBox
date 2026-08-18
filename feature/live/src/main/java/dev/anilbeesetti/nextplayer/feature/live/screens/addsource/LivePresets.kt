package dev.anilbeesetti.nextplayer.feature.live.screens.addsource

import androidx.annotation.StringRes
import dev.anilbeesetti.nextplayer.core.ui.R

/**
 * A built-in playlist offered on the add screen. Selecting one fills the form so that a long URL
 * never has to be typed with a remote control.
 */
data class LivePreset(
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val url: String,
)

val livePresets = listOf(
    LivePreset(
        nameRes = R.string.preset_iptv_org_chinese,
        descriptionRes = R.string.preset_iptv_org_chinese_description,
        url = "https://iptv-org.github.io/iptv/languages/zho.m3u",
    ),
    LivePreset(
        nameRes = R.string.preset_iptv_org_china,
        descriptionRes = R.string.preset_iptv_org_china_description,
        url = "https://iptv-org.github.io/iptv/countries/cn.m3u",
    ),
    LivePreset(
        nameRes = R.string.preset_fanmingming_ipv6,
        descriptionRes = R.string.preset_fanmingming_ipv6_description,
        url = "https://live.fanmingming.com/tv/m3u/ipv6.m3u",
    ),
)
