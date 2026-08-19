package dev.anilbeesetti.nextplayer.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MediaDatabase::class.java,
    )

    @Test
    fun migrate8To9_preservesConnectionAndAddsPasswordDefaults() {
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                "INSERT INTO network_connection " +
                    "(id,name,protocol,host,port,path,username,password,use_https,created_at) " +
                    "VALUES (1,'NAS','FTP','10.0.2.2',2121,'/media','alice','secret',0,123)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            9,
            true,
            MediaDatabase.MIGRATION_8_9,
        ).use { db ->
            db.query("SELECT * FROM network_connection WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                assertEquals("NAS", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                assertEquals("FTP", cursor.getString(cursor.getColumnIndexOrThrow("protocol")))
                assertEquals("10.0.2.2", cursor.getString(cursor.getColumnIndexOrThrow("host")))
                assertEquals(2121, cursor.getInt(cursor.getColumnIndexOrThrow("port")))
                assertEquals("/media", cursor.getString(cursor.getColumnIndexOrThrow("path")))
                assertEquals("alice", cursor.getString(cursor.getColumnIndexOrThrow("username")))
                assertEquals("secret", cursor.getString(cursor.getColumnIndexOrThrow("password")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("use_https")))
                assertEquals(123L, cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                assertEquals(
                    "PASSWORD",
                    cursor.getString(cursor.getColumnIndexOrThrow("authentication")),
                )
                assertEquals(
                    "",
                    cursor.getString(cursor.getColumnIndexOrThrow("private_key_file_name")),
                )
                assertEquals(
                    "",
                    cursor.getString(cursor.getColumnIndexOrThrow("private_key_passphrase")),
                )
                assertEquals(
                    "",
                    cursor.getString(cursor.getColumnIndexOrThrow("host_key_fingerprint")),
                )
            }
        }
    }

    @Test
    fun migrate9To10_addsLiveSourceTableAndKeepsExistingData() {
        helper.createDatabase(TEST_DB, 9).apply {
            execSQL(
                "INSERT INTO network_connection " +
                    "(id,name,protocol,host,port,path,username,password,use_https,created_at," +
                    "authentication,private_key_file_name,private_key_passphrase,host_key_fingerprint) " +
                    "VALUES (1,'NAS','SMB','10.0.2.2',445,'media','alice','secret',0,123," +
                    "'PASSWORD','','','')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            10,
            true,
            MediaDatabase.MIGRATION_9_10,
        ).use { db ->
            db.execSQL(
                "INSERT INTO live_source (id,name,url,created_at) " +
                    "VALUES (1,'IPTV','https://example.com/tv.m3u',456)",
            )
            db.query("SELECT * FROM live_source WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("IPTV", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                assertEquals(
                    "https://example.com/tv.m3u",
                    cursor.getString(cursor.getColumnIndexOrThrow("url")),
                )
                assertEquals(456L, cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
            }
            db.query("SELECT name FROM network_connection WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("NAS", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate10To11_addsHistoryColumnsAndKeepsPlaybackState() {
        helper.createDatabase(TEST_DB, 10).apply {
            execSQL(
                "INSERT INTO media_state " +
                    "(uri,playback_position,audio_track_index,subtitle_track_index,playback_speed," +
                    "last_played_time,external_subs,video_scale,subtitle_delay,subtitle_speed) " +
                    "VALUES ('content://media/1',49769,NULL,NULL,NULL,123,'',1.0,0,1.0)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            11,
            true,
            MediaDatabase.MIGRATION_10_11,
        ).use { db ->
            db.query("SELECT * FROM media_state WHERE uri = 'content://media/1'").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(49769L, cursor.getLong(cursor.getColumnIndexOrThrow("playback_position")))
                assertEquals(123L, cursor.getLong(cursor.getColumnIndexOrThrow("last_played_time")))
                check(cursor.isNull(cursor.getColumnIndexOrThrow("title")))
                check(cursor.isNull(cursor.getColumnIndexOrThrow("duration")))
            }
        }
    }

    @Test
    fun migrate11To12_forgetsLiveChannelsAndKeepsEveryOtherKindOfItem() {
        helper.createDatabase(TEST_DB, 11).apply {
            listOf(
                "content://media/external/video/media/42",
                "file:///storage/emulated/0/Movies/holiday.mkv",
                "cafeplayer-network://1/movies/holiday.mkv",
                "http://192.168.1.10:4022/cctv1.m3u8",
                "https://live.example.com/cctv2.m3u8",
            ).forEach { uri ->
                execSQL(
                    "INSERT INTO media_state " +
                        "(uri,playback_position,audio_track_index,subtitle_track_index," +
                        "playback_speed,last_played_time,external_subs,video_scale," +
                        "subtitle_delay,subtitle_speed,title,duration) " +
                        "VALUES ('$uri',0,NULL,NULL,NULL,123,'',1.0,0,1.0,'Watched',NULL)",
                )
            }
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            12,
            true,
            MediaDatabase.MIGRATION_11_12,
        ).use { db ->
            db.query("SELECT uri FROM media_state ORDER BY uri").use { cursor ->
                val remaining = buildList {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
                assertEquals(
                    listOf(
                        "cafeplayer-network://1/movies/holiday.mkv",
                        "content://media/external/video/media/42",
                        "file:///storage/emulated/0/Movies/holiday.mkv",
                    ),
                    remaining,
                )
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
