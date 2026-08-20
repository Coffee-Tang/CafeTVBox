package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.model.LibraryWork
import dev.anilbeesetti.nextplayer.core.model.WorkDetail
import dev.anilbeesetti.nextplayer.core.model.WorkKind
import kotlinx.coroutines.flow.Flow

interface CatalogueRepository {

    fun observeLibrariesExist(): Flow<Boolean>

    fun observeWorks(): Flow<List<LibraryWork>>

    fun observeWork(workId: Long): Flow<WorkDetail?>

    /**
     * Declares a library at [root] on [connectionId], walks it, writes the works it holds, and
     * binds any work a catalogue search can answer without asking.
     */
    suspend fun addLibraryAndScan(
        name: String,
        root: String,
        kind: WorkKind,
        connectionId: Long,
    ): Result<Unit>

    /**
     * Walks every declared library again. Works already bound, especially by the viewer, are left
     * alone; only new works and unbound ones are searched for.
     */
    suspend fun reconcile()

    /**
     * The media key a work should carry on from: the file its last-watched episode was played
     * from, or its first episode when none has been watched. Null when the work has no files.
     */
    suspend fun keyToResume(workId: Long): String?

    /** The media key of the file an episode should open on, or null when it has no files. */
    suspend fun playableKey(itemId: Long): String?
}
