package dev.anilbeesetti.nextplayer.core.model.search

import java.util.concurrent.ConcurrentHashMap
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

/**
 * How a Chinese name can be typed on a keyboard that only offers letters.
 *
 * Entering Chinese with a remote control means walking a candidate list with the arrow keys, which
 * is slow enough that people avoid searching at all. Letters are one press each, so a viewer looking
 * for 硅谷 types `guigu`, or `gg` when they can only be bothered with the initials.
 *
 * [syllables] keeps the readings apart — `gui`, `gu` — rather than running them together, because
 * where one ends matters: 中国 read as `zhongguo` appears to contain `gg`, which would answer for
 * 硅谷 without having anything to do with it. [initials] keeps their first letters — `gg`. Anything
 * that is not Chinese is carried through as a piece of its own, so `硅谷2016` has the syllables
 * `gui`, `gu`, `2016`.
 */
data class PinyinForms(
    val syllables: List<String>,
    val initials: String,
)

/**
 * The pinyin forms of [text], or null when there is no Chinese in it to spell out.
 *
 * A character with several readings is spelled with its first, since which one is meant cannot be
 * told from a title. It costs the odd name — 长江 is spelled `changjiang`, but 长 read `zhang` in
 * someone's name would be spelled wrong — and the literal name still matches either way.
 */
fun pinyinOf(text: String): PinyinForms? = cache.getOrPut(text) { text.spelledOut() ?: EMPTY }.takeIf { it != EMPTY }

private fun String.spelledOut(): PinyinForms? {
    val syllables = mutableListOf<String>()
    val initials = StringBuilder()
    var plain = StringBuilder()
    var sawChinese = false

    fun flushPlain() {
        if (plain.isEmpty()) return
        syllables.add(plain.toString())
        plain = StringBuilder()
    }

    for (character in this) {
        val reading = PinyinHelper.toHanyuPinyinStringArray(character, format)?.firstOrNull()
        if (reading == null) {
            plain.append(character.lowercaseChar())
            initials.append(character.lowercaseChar())
        } else {
            flushPlain()
            sawChinese = true
            syllables.add(reading)
            initials.append(reading.first())
        }
    }
    flushPlain()

    return if (sawChinese) PinyinForms(syllables, initials.toString()) else null
}

private val format = HanyuPinyinOutputFormat().apply {
    caseType = HanyuPinyinCaseType.LOWERCASE
    toneType = HanyuPinyinToneType.WITHOUT_TONE
    vCharType = HanyuPinyinVCharType.WITH_V
}

/**
 * Spelling a name out is the same work every keystroke, for names that do not change, so each is
 * spelled once. What is held is the titles of a library and the names of a channel list, both of
 * which the viewer is already keeping in memory to look at.
 */
private val cache = ConcurrentHashMap<String, PinyinForms>()

/** Stands for "there is no Chinese here", since the cache cannot hold a null. */
private val EMPTY = PinyinForms(emptyList(), "")
