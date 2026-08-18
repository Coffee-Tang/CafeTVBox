package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.database.dao.LiveSourceDao
import dev.anilbeesetti.nextplayer.core.database.entities.LiveSourceEntity
import dev.anilbeesetti.nextplayer.core.model.LiveSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalLiveSourceRepositoryTest {
    private val dao = FakeLiveSourceDao()
    private val repository = LocalLiveSourceRepository(dao)

    @Test
    fun `upsert and get source round trips all fields`() = runTest {
        val source = LiveSource(
            name = "IPTV",
            url = "https://live.fanmingming.com/tv/m3u/ipv6.m3u",
            createdAt = 100,
        )

        val id = repository.upsert(source)

        assertEquals(1L, id)
        assertEquals(source.copy(id = 1), repository.getSource(1))
    }

    @Test
    fun `getSources emits stored sources`() = runTest {
        repository.upsert(LiveSource(name = "A", url = "https://a/tv.m3u"))

        val sources = repository.getSources().first()

        assertEquals(1, sources.size)
        assertEquals("A", sources.first().name)
    }

    @Test
    fun `upsert with existing id updates the source`() = runTest {
        val id = repository.upsert(LiveSource(name = "Old", url = "https://old/tv.m3u"))

        repository.upsert(LiveSource(id = id, name = "New", url = "https://new/tv.m3u"))

        assertEquals("New", repository.getSource(id)?.name)
        assertEquals("https://new/tv.m3u", repository.getSource(id)?.url)
        assertEquals(1, repository.getSources().first().size)
    }

    @Test
    fun `delete removes the source`() = runTest {
        val id = repository.upsert(LiveSource(name = "Temp", url = "https://temp/tv.m3u"))

        repository.delete(id)

        assertNull(repository.getSource(id))
    }

    private class FakeLiveSourceDao : LiveSourceDao {
        private val sources = MutableStateFlow<List<LiveSourceEntity>>(emptyList())

        override suspend fun upsert(source: LiveSourceEntity): Long {
            val id = source.id.takeIf { it != 0L } ?: 1L
            val savedSource = source.copy(id = id)
            sources.value = sources.value.filterNot { it.id == id } + savedSource
            return id
        }

        override fun getAll(): Flow<List<LiveSourceEntity>> = sources

        override suspend fun getById(id: Long): LiveSourceEntity? =
            sources.value.firstOrNull { it.id == id }

        override suspend fun deleteById(id: Long) {
            sources.value = sources.value.filterNot { it.id == id }
        }
    }
}
