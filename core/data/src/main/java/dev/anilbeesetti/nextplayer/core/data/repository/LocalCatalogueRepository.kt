package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.catalogue.TmdbCatalogue
import dev.anilbeesetti.nextplayer.core.data.catalogue.collectLibraryEntries
import dev.anilbeesetti.nextplayer.core.data.catalogue.toEpisodes
import dev.anilbeesetti.nextplayer.core.database.dao.CatalogueDao
import dev.anilbeesetti.nextplayer.core.database.entities.CatalogueItemEntity
import dev.anilbeesetti.nextplayer.core.database.entities.CatalogueItemFileEntity
import dev.anilbeesetti.nextplayer.core.database.entities.CatalogueLibraryEntity
import dev.anilbeesetti.nextplayer.core.database.entities.CatalogueWorkEntity
import dev.anilbeesetti.nextplayer.core.media.network.NetworkClientFactory
import dev.anilbeesetti.nextplayer.core.media.network.NetworkMediaKey
import dev.anilbeesetti.nextplayer.core.model.GroupedItem
import dev.anilbeesetti.nextplayer.core.model.GroupedLibrary
import dev.anilbeesetti.nextplayer.core.model.LibraryWork
import dev.anilbeesetti.nextplayer.core.model.WorkDetail
import dev.anilbeesetti.nextplayer.core.model.WorkKind
import dev.anilbeesetti.nextplayer.core.model.autoBind
import dev.anilbeesetti.nextplayer.core.model.episodeToResume
import dev.anilbeesetti.nextplayer.core.model.groupLibrary
import dev.anilbeesetti.nextplayer.core.model.searchName
import dev.anilbeesetti.nextplayer.core.model.tmdbImageUrl
import dev.anilbeesetti.nextplayer.core.model.workDetailOf
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class LocalCatalogueRepository @Inject constructor(
    private val dao: CatalogueDao,
    private val connections: NetworkConnectionRepository,
    private val clients: NetworkClientFactory,
    private val catalogue: TmdbCatalogue,
) : CatalogueRepository {

    override fun observeLibrariesExist(): Flow<Boolean> =
        dao.observeLibraries().map { it.isNotEmpty() }

    override fun observeWorks(): Flow<List<LibraryWork>> =
        dao.observeWorks().map { works -> works.map { it.toModel() } }

    override fun observeWork(workId: Long): Flow<WorkDetail?> =
        combine(dao.observeWork(workId), dao.observeWorkPlayback(workId)) { work, playback ->
            val entity = work ?: return@combine null
            workDetailOf(entity.toModel(), playback.toEpisodes())
        }

    override suspend fun addLibraryAndScan(
        name: String,
        root: String,
        kind: WorkKind,
        connectionId: Long,
    ): Result<Unit> = runCatching {
        val existing = dao.findLibrary(connectionId, root)
        val libraryId = existing?.id ?: dao.upsertLibrary(
            CatalogueLibraryEntity(
                name = name,
                root = root,
                kind = kind.name,
                connectionId = connectionId,
            ),
        )
        scanLibrary(libraryId, connectionId, root, kind)
    }

    override suspend fun reconcile() {
        for (library in dao.getLibraries()) {
            val connectionId = library.connectionId ?: continue
            val kind = runCatching { WorkKind.valueOf(library.kind) }.getOrNull() ?: continue
            runCatching { scanLibrary(library.id, connectionId, library.root, kind) }
        }
    }

    override suspend fun keyToResume(workId: Long): String? =
        dao.observeWorkPlayback(workId).first().toEpisodes().episodeToResume()?.mediaKey

    override suspend fun playableKey(itemId: Long): String? {
        val item = dao.getItem(itemId) ?: return null
        return dao.observeWorkPlayback(item.workId).first().toEpisodes()
            .firstOrNull { it.id == itemId }
            ?.mediaKey
    }

    private suspend fun scanLibrary(
        libraryId: Long,
        connectionId: Long,
        root: String,
        kind: WorkKind,
    ) {
        val connection = connections.getConnection(connectionId) ?: error("Connection $connectionId is gone")
        val client = clients.create(connection)
        try {
            if (!client.isConnected()) {
                client.connect().getOrThrow()
            }
            val entries = collectLibraryEntries(client::listFiles, root)
            val grouped = groupLibrary(entries, kind)
            persist(libraryId, connectionId, grouped)
            scrapeUnbound(libraryId)
        } finally {
            runCatching { client.disconnect() }
        }
    }

    private suspend fun persist(libraryId: Long, connectionId: Long, grouped: GroupedLibrary) {
        val keepWorkIds = mutableListOf<Long>()
        for (work in grouped.works) {
            val existing = dao.findWork(libraryId, work.kind.name, work.workKey)
            val workId = dao.upsertWork(
                CatalogueWorkEntity(
                    id = existing?.id ?: 0,
                    libraryId = libraryId,
                    workKey = work.workKey,
                    kind = work.kind.name,
                    title = existing?.title ?: work.title,
                    otherTitle = existing?.otherTitle ?: work.otherTitle,
                    year = existing?.year ?: work.year,
                    tmdbId = existing?.tmdbId,
                    posterPath = existing?.posterPath,
                    overview = existing?.overview,
                    boundBy = existing?.boundBy,
                ),
            )
            keepWorkIds += workId
            persistItems(workId, connectionId, work.items)
        }
        if (keepWorkIds.isEmpty()) {
            dao.deleteWorksOfLibrary(libraryId)
        } else {
            dao.deleteWorksNotIn(libraryId, keepWorkIds)
        }
    }

    private suspend fun persistItems(
        workId: Long,
        connectionId: Long,
        items: List<GroupedItem>,
    ) {
        val keepItemIds = mutableListOf<Long>()
        for (item in items) {
            val existing = dao.findItem(workId, item.season, item.episode)
            val itemId = dao.upsertItem(
                CatalogueItemEntity(
                    id = existing?.id ?: 0,
                    workId = workId,
                    season = item.season,
                    episode = item.episode,
                    playbackPosition = existing?.playbackPosition ?: 0,
                    duration = existing?.duration,
                    lastPlayedTime = existing?.lastPlayedTime,
                    lastFileUri = existing?.lastFileUri,
                ),
            )
            keepItemIds += itemId
            dao.deleteFilesOfItem(itemId)
            for (file in item.files) {
                dao.upsertFile(
                    CatalogueItemFileEntity(
                        itemId = itemId,
                        uri = NetworkMediaKey(connectionId, file.path).toString(),
                        path = file.path,
                    ),
                )
            }
        }
        if (keepItemIds.isNotEmpty()) {
            dao.deleteItemsNotIn(workId, keepItemIds)
        }
    }

    private suspend fun scrapeUnbound(libraryId: Long) {
        for (work in dao.getWorks(libraryId)) {
            if (work.tmdbId != null || work.boundBy == BOUND_BY_USER) continue
            val kind = runCatching { WorkKind.valueOf(work.kind) }.getOrNull() ?: continue
            val matches = catalogue.search(searchName(kind, work.title, work.otherTitle, work.year))
                .getOrNull() ?: continue
            val bound = autoBind(matches) ?: continue
            dao.upsertWork(
                work.copy(
                    tmdbId = bound.id,
                    posterPath = bound.posterPath,
                    overview = bound.overview.ifBlank { null },
                    title = bound.title.ifBlank { work.title },
                    otherTitle = bound.originalTitle.takeIf { it.isNotBlank() && it != bound.title }
                        ?: work.otherTitle,
                    year = bound.year ?: work.year,
                    boundBy = BOUND_BY_AUTO,
                ),
            )
            delay(SCRAPE_GAP)
        }
    }

    private fun CatalogueWorkEntity.toModel() = LibraryWork(
        id = id,
        libraryId = libraryId,
        workKey = workKey,
        kind = runCatching { WorkKind.valueOf(kind) }.getOrDefault(WorkKind.SERIES),
        title = title,
        otherTitle = otherTitle,
        year = year,
        posterUrl = tmdbImageUrl(posterPath),
    )

    private companion object {
        const val BOUND_BY_AUTO = "AUTO"
        const val BOUND_BY_USER = "USER"
        val SCRAPE_GAP = 300L
    }
}
