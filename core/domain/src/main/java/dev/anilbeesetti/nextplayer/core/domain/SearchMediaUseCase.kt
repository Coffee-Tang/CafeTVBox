package dev.anilbeesetti.nextplayer.core.domain

import dev.anilbeesetti.nextplayer.core.common.Dispatcher
import dev.anilbeesetti.nextplayer.core.common.NextDispatchers
import dev.anilbeesetti.nextplayer.core.model.Folder
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.model.search.rankedBy
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

data class SearchResults(
    val folders: List<Folder> = emptyList(),
    val videos: List<Video> = emptyList(),
) {
    val isEmpty: Boolean
        get() = folders.isEmpty() && videos.isEmpty()

    val totalCount: Int
        get() = folders.size + videos.size
}

/**
 * The files on the device itself that a query names.
 *
 * This is the system's index of local storage, so it holds nothing of a share or a channel; those
 * are searched where they are kept.
 */
class SearchMediaUseCase @Inject constructor(
    private val getSortedVideosUseCase: GetSortedVideosUseCase,
    private val getSortedFoldersUseCase: GetSortedFoldersUseCase,
    @Dispatcher(NextDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(query: String): Flow<SearchResults> {
        if (query.isBlank()) return flowOf(SearchResults())

        return combine(
            getSortedVideosUseCase(),
            getSortedFoldersUseCase(),
        ) { videos, folders ->
            SearchResults(
                folders = folders.rankedBy(query) { listOf(it.name, it.path) },
                videos = videos.rankedBy(query) { listOf(it.nameWithExtension, it.path) },
            )
        }.flowOn(defaultDispatcher)
    }
}
