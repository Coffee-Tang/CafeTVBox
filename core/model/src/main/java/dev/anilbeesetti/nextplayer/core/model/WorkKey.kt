package dev.anilbeesetti.nextplayer.core.model

/**
 * Reduces a work's title to a key that identifies it wherever a file names it.
 *
 * A library writes the same work as `硅谷`, `Silicon Valley` and `硅谷.Silicon.Valley`; those are
 * one work if they are treated as aliases of each other, and three if they are compared as written.
 * The key folds spelling so that aliases can be recognised. What the viewer reads stays the title
 * the files gave.
 *
 * Two titles that name different works in different languages, and share no alias, stay apart:
 * folding punctuation cannot tell them they are the same, any more than it can tell `广东卫视`
 * from `Guangdong Satellite TV`.
 */
fun workKey(title: String): String = title.lowercase().filter(Char::isLetterOrDigit)
