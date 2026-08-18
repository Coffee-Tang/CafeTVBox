package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.database.dao.LiveSourceDao
import dev.anilbeesetti.nextplayer.core.database.entities.LiveSourceEntity
import dev.anilbeesetti.nextplayer.core.model.LiveSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LocalLiveSourceRepository @Inject constructor(
    private val liveSourceDao: LiveSourceDao,
) : LiveSourceRepository {

    override fun getSources(): Flow<List<LiveSource>> =
        liveSourceDao.getAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun getSource(id: Long): LiveSource? =
        liveSourceDao.getById(id)?.toModel()

    override suspend fun upsert(source: LiveSource): Long =
        liveSourceDao.upsert(source.toEntity())

    override suspend fun delete(id: Long) = liveSourceDao.deleteById(id)

    private fun LiveSourceEntity.toModel() = LiveSource(
        id = id,
        name = name,
        url = url,
        createdAt = createdAt,
    )

    private fun LiveSource.toEntity() = LiveSourceEntity(
        id = id,
        name = name,
        url = url,
        createdAt = createdAt,
    )
}
