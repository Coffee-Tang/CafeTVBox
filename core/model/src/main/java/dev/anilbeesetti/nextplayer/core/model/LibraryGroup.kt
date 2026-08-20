package dev.anilbeesetti.nextplayer.core.model

/**
 * A media file as a library scan presents it: the names [mediaName] reads, and the path that
 * later finds the file again.
 */
data class LibraryEntry(
    val fileName: String,
    val folderName: String,
    val path: String,
)

/** The works a scan of a library produced, and the files that did not become any of them. */
data class GroupedLibrary(
    val works: List<GroupedWork>,
    val unread: List<LibraryEntry>,
)

/** One work, the items it holds, and the titles the files named it by. */
data class GroupedWork(
    val workKey: String,
    val kind: WorkKind,
    val title: String,
    val otherTitle: String?,
    /**
     * The year a film's files agree on, and null for a series — a series file dates its season,
     * which is not a year the work can be filed under.
     */
    val year: Int?,
    /** Every key a file of this work answered to, so a later scan can find it under either title. */
    val aliases: Set<String>,
    val items: List<GroupedItem>,
)

/** One film, or one episode, and the files that hold it. */
data class GroupedItem(
    val season: Int,
    val episode: Int,
    val files: List<LibraryEntry>,
) {
    companion object {
        /**
         * A film is one item and is not numbered, so both fields stay at zero. Zero is not a
         * season a series uses: [mediaName] reads a missing season as 1.
         */
        const val FILM_SEASON = 0
        const val FILM_EPISODE = 0
    }
}

/**
 * Folds [entries] into works by the titles their names give, and drops anything that does not
 * name one — or that names the wrong [kind], when a library has declared itself as films or as
 * series.
 *
 * Files that share a title, or that carry each other's title as the other language, become one
 * work: `硅谷.Silicon.Valley.S03E01` and a file that only says `硅谷` are the same series. A film
 * and a series of the same title stay apart, because they are different kinds of work. Two files
 * of the same episode become two files of one item, which is how a 1080p and a 4K of one episode
 * share its progress later.
 *
 * The folder a file sits in is only what [mediaName] already consults. Six season folders of one
 * series therefore become one work, which is the point of grouping by title rather than by folder.
 */
fun groupLibrary(
    entries: List<LibraryEntry>,
    kind: WorkKind? = null,
): GroupedLibrary {
    val unread = mutableListOf<LibraryEntry>()
    val parsed = ArrayList<Parsed>(entries.size)
    for (entry in entries) {
        val name = mediaName(entry.fileName, entry.folderName)
        when {
            name == null -> unread += entry
            kind != null && name.kind != kind -> unread += entry
            else -> parsed += Parsed(entry, name)
        }
    }

    val buckets = mutableListOf<Bucket>()
    val byAlias = mutableMapOf<Alias, Bucket>()
    for (item in parsed) {
        val aliases = aliasesOf(item.name)
        if (aliases.isEmpty()) {
            unread += item.entry
            continue
        }
        val found = aliases.mapNotNull { byAlias[Alias(item.name.kind, it)] }.distinct()
        val bucket = when (found.size) {
            0 -> Bucket(item.name.kind).also { buckets += it }
            1 -> found[0]
            else -> found.mergeIntoFirst(buckets)
        }
        bucket.members += item
        bucket.indexAliases(byAlias)
    }

    return GroupedLibrary(
        works = buckets.map { it.toWork() }.sortedWith(compareBy(GroupedWork::title, GroupedWork::workKey)),
        unread = unread,
    )
}

private data class Parsed(val entry: LibraryEntry, val name: MediaName)

private data class Alias(val kind: WorkKind, val key: String)

private class Bucket(val kind: WorkKind, val members: MutableList<Parsed> = mutableListOf())

private fun aliasesOf(name: MediaName): List<String> =
    listOfNotNull(name.title, name.otherTitle).map(::workKey).filter(String::isNotEmpty)

private fun List<Bucket>.mergeIntoFirst(buckets: MutableList<Bucket>): Bucket {
    val keeper = first()
    for (other in drop(1)) {
        keeper.members += other.members
        buckets.remove(other)
    }
    return keeper
}

private fun Bucket.indexAliases(byAlias: MutableMap<Alias, Bucket>) {
    for (member in members) {
        for (alias in aliasesOf(member.name)) {
            byAlias[Alias(kind, alias)] = this
        }
    }
}

private fun Bucket.toWork(): GroupedWork {
    val names = members.map { it.name }
    val bilingual = names.firstOrNull { it.otherTitle != null }
    val title = bilingual?.title ?: names.first().title
    val otherTitle = bilingual?.otherTitle
        ?: names.map { it.title }.firstOrNull { it != title }
    val aliases = names.flatMap(::aliasesOf).toSet()
    val latinKey = aliases.firstOrNull { alias -> alias.any { it in 'a'..'z' } }
    return GroupedWork(
        workKey = latinKey ?: aliases.minOrNull() ?: workKey(title),
        kind = kind,
        title = title,
        otherTitle = otherTitle,
        year = yearOf(names),
        aliases = aliases,
        items = members.toItems(),
    )
}

private fun yearOf(names: List<MediaName>): Int? {
    val years = names.mapNotNull { (it as? MediaName.Film)?.year }.distinct()
    return years.singleOrNull()
}

private fun List<Parsed>.toItems(): List<GroupedItem> =
    groupBy { it.numbering() }
        .map { (number, group) ->
            GroupedItem(
                season = number.first,
                episode = number.second,
                files = group.map { it.entry },
            )
        }
        .sortedWith(compareBy(GroupedItem::season, GroupedItem::episode))

private fun Parsed.numbering(): Pair<Int, Int> = when (val name = name) {
    is MediaName.Film -> GroupedItem.FILM_SEASON to GroupedItem.FILM_EPISODE
    is MediaName.Episode -> name.season to name.episode
}
