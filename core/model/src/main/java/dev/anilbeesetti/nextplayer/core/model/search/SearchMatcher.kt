package dev.anilbeesetti.nextplayer.core.model.search

/**
 * How well a name answers what was typed, as a score to sort by, or zero for not at all.
 *
 * Ranked so that the closer a name is to being what was typed, the higher it sits:
 *  - the whole query appears as it was typed — `stranger 2019` in `stranger 2019`
 *  - every word appears in order, with anything in between — `stranger 2019` in `stranger things 2019`
 *  - every word appears somewhere — `stranger 2019` in `2019 stranger things`
 *
 * A name can also be reached by its pinyin, which scores well below the same match on the name
 * itself, so a viewer who types what they see never has it buried under something spelled out.
 */
class SearchMatcher(query: String) {

    private val queryLower = query.trim().lowercase()
    private val queryWords = queryLower.split(Regex("\\s+")).filter { it.isNotBlank() }

    private val wordsInOrder: Regex? = if (queryWords.size > 1) {
        Regex(queryWords.joinToString(".*") { Regex.escape(it) }, RegexOption.IGNORE_CASE)
    } else {
        null
    }

    /**
     * The best score any of [texts] reaches, so that a name and the path it sits at, or a title and
     * the title it is also known by, count as ways of naming the same thing rather than as rivals.
     */
    fun score(vararg texts: String?): Int = texts.filterNotNull().maxOfOrNull { scoreOne(it) } ?: 0

    private fun scoreOne(text: String): Int {
        val literal = scoreText(text.lowercase())
        if (literal > 0) return literal
        return pinyinOf(text)?.let { scorePinyin(it) } ?: 0
    }

    /**
     * What a name spelled out is worth, which is well under what the name itself is worth.
     *
     * The initials have to be typed from the start — someone typing `gg` means a name beginning
     * that way, not any name with those letters somewhere inside — and syllables have to be typed
     * from the start of one, so that `zhong guo` is not read as holding `gg`.
     */
    private fun scorePinyin(pinyin: PinyinForms): Int {
        val typed = queryLower.replace(" ", "")
        if (typed.isEmpty()) return 0

        val onInitials = when {
            pinyin.initials == typed -> 120
            pinyin.initials.startsWith(typed) -> 100
            else -> 0
        }

        val onSyllables = pinyin.syllables.indices.maxOfOrNull { from ->
            val rest = pinyin.syllables.subList(from, pinyin.syllables.size).joinToString("")
            when {
                rest == typed -> if (from == 0) 120 else 90
                rest.startsWith(typed) -> if (from == 0) 100 else 80
                else -> 0
            }
        } ?: 0

        return maxOf(onInitials, onSyllables)
    }

    private fun scoreText(text: String): Int {
        if (queryWords.isEmpty()) return 0

        if (text.contains(queryLower)) {
            val atWordStart = if (startsWord(text, queryLower)) 50 else 0
            val atTextStart = if (text.startsWith(queryLower)) 30 else 0
            return 1000 + atWordStart + atTextStart
        }

        if (queryWords.size == 1) return 0

        if (wordsInOrder?.containsMatchIn(text) == true) return 500

        if (queryWords.all { text.contains(it) }) {
            val wordsAtStart = queryWords.count { startsWord(text, it) }
            return 200 + wordsAtStart * 20
        }

        return 0
    }

    /** Whether [part] begins a word of [text], which is a nearer thing than landing mid-word. */
    private fun startsWord(text: String, part: String): Boolean {
        val at = text.indexOf(part)
        return when {
            at == -1 -> false
            at == 0 -> true
            else -> text[at - 1] in wordSeparators
        }
    }

    private companion object {
        val wordSeparators = charArrayOf(' ', '_', '-', '.', '/', '\\', '[', ']', '(', ')')
    }
}
