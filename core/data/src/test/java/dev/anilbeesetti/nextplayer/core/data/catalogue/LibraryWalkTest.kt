package dev.anilbeesetti.nextplayer.core.data.catalogue

import dev.anilbeesetti.nextplayer.core.model.NetworkFile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryWalkTest {

    @Test
    fun `videos under season folders are collected and pictures are not`() = runTest {
        val tree = mapOf(
            "/Shows" to listOf(
                dir("SE03（10集）1080P", "/Shows/SE03（10集）1080P"),
                dir("天道【4K超高清修复】", "/Shows/天道【4K超高清修复】"),
                dir(".cafeplayer", "/Shows/.cafeplayer"),
            ),
            "/Shows/SE03（10集）1080P" to listOf(
                file("硅谷.Silicon.Valley.2016.S03E01.mkv", "/Shows/SE03（10集）1080P/硅谷.Silicon.Valley.2016.S03E01.mkv"),
            ),
            "/Shows/天道【4K超高清修复】" to listOf(
                file("[4K超高清修复]无删减完整版第01集_超清 4K.mp4", "/Shows/天道【4K超高清修复】/第01集.mp4"),
                file("海报.jpg", "/Shows/天道【4K超高清修复】/海报.jpg"),
            ),
            "/Shows/.cafeplayer" to listOf(
                file("poster.jpg", "/Shows/.cafeplayer/poster.jpg"),
            ),
        )

        val entries = collectLibraryEntries(
            listFiles = { path -> Result.success(tree[path].orEmpty()) },
            rootPath = "/Shows",
        )

        assertEquals(
            listOf(
                "硅谷.Silicon.Valley.2016.S03E01.mkv" to "SE03（10集）1080P",
                "[4K超高清修复]无删减完整版第01集_超清 4K.mp4" to "天道【4K超高清修复】",
            ),
            entries.map { it.fileName to it.folderName },
        )
    }

    private fun dir(name: String, path: String) =
        NetworkFile(name = name, path = path, isDirectory = true)

    private fun file(name: String, path: String) =
        NetworkFile(name = name, path = path, isDirectory = false)
}
