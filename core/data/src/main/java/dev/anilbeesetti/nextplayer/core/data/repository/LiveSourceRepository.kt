package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.model.LiveSource
import kotlinx.coroutines.flow.Flow

interface LiveSourceRepository {

    fun getSources(): Flow<List<LiveSource>>

    suspend fun getSource(id: Long): LiveSource?

    /** Inserts or updates [source] and returns its row id. */
    suspend fun upsert(source: LiveSource): Long

    suspend fun delete(id: Long)
}
