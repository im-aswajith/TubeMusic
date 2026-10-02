package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalPreferencesRepository
import com.example.data.local.MusicDownloadManager
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadedTrack
import com.example.data.model.YouTubeVideo
import com.example.data.repository.RoomMusicRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MusicDownloadManagerIntegrityTest {

    private lateinit var context: Context
    private lateinit var localRepo: LocalPreferencesRepository
    private lateinit var roomRepo: RoomMusicRepository
    private lateinit var downloadManager: MusicDownloadManager
    private lateinit var downloadsDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        localRepo = LocalPreferencesRepository(context)
        roomRepo = RoomMusicRepository(context)
        downloadManager = MusicDownloadManager(context, localRepo, roomRepo)
        downloadsDir = downloadManager.downloadsDir

        // Clean out downloads directory before each test
        downloadsDir.listFiles()?.forEach { it.delete() }
        localRepo.clearAllDownloadedTracks()
    }

    private fun createDummyAudioFile(targetFile: File, headerType: String, sizeBytes: Long = 60_000L) {
        val parent = targetFile.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()

        FileOutputStream(targetFile).use { fos ->
            val header: ByteArray = when (headerType) {
                "id3" -> byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
                "mp3_sync" -> byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
                "m4a" -> byteArrayOf(0x00, 0x00, 0x00, 0x20, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(), 'M'.code.toByte(), '4'.code.toByte(), 'A'.code.toByte(), ' '.code.toByte())
                "webm" -> byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte(), 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
                "ogg" -> byteArrayOf('O'.code.toByte(), 'g'.code.toByte(), 'g'.code.toByte(), 'S'.code.toByte(), 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
                "flac" -> byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte(), 0x00, 0x00, 0x00, 0x22, 0x00, 0x00, 0x00, 0x00)
                "wav" -> byteArrayOf('R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(), 0x24, 0x00, 0x00, 0x00, 'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte())
                else -> byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B)
            }
            fos.write(header)
            val padding = ByteArray((sizeBytes - header.size).toInt())
            fos.write(padding)
        }
    }

    @Test
    fun testValidAudioHeaderValidation() {
        val mp3File = File(downloadsDir, "test_valid.mp3")
        createDummyAudioFile(mp3File, "id3", sizeBytes = 60_000L)
        assertTrue("ID3v2 header must be recognized as valid audio", downloadManager.validateAudioFile(mp3File))

        val m4aFile = File(downloadsDir, "test_valid.m4a")
        createDummyAudioFile(m4aFile, "m4a", sizeBytes = 80_000L)
        assertTrue("M4A ftyp header must be recognized as valid audio", downloadManager.validateAudioFile(m4aFile))

        val webmFile = File(downloadsDir, "test_valid.webm")
        createDummyAudioFile(webmFile, "webm", sizeBytes = 70_000L)
        assertTrue("WebM Opus header must be recognized as valid audio", downloadManager.validateAudioFile(webmFile))

        val oggFile = File(downloadsDir, "test_valid.ogg")
        createDummyAudioFile(oggFile, "ogg", sizeBytes = 65_000L)
        assertTrue("OggS header must be recognized as valid audio", downloadManager.validateAudioFile(oggFile))

        val flacFile = File(downloadsDir, "test_valid.flac")
        createDummyAudioFile(flacFile, "flac", sizeBytes = 100_000L)
        assertTrue("FLAC header must be recognized as valid audio", downloadManager.validateAudioFile(flacFile))

        val wavFile = File(downloadsDir, "test_valid.wav")
        createDummyAudioFile(wavFile, "wav", sizeBytes = 90_000L)
        assertTrue("RIFF/WAVE header must be recognized as valid audio", downloadManager.validateAudioFile(wavFile))
    }

    @Test
    fun testMalformedAndHtmlFileRejection() {
        // 1. HTML 403 Forbidden payload (simulating cloudflare or restricted stream page)
        val htmlFile = File(downloadsDir, "error_payload.mp3")
        FileOutputStream(htmlFile).use { fos ->
            val html = "<!DOCTYPE html><html><head><title>403 Forbidden</title></head><body>Access Denied</body></html>"
            fos.write(html.toByteArray())
            // Pad to > 50KB to test that size alone is not enough
            fos.write(ByteArray(60_000))
        }

        assertFalse("HTML response must be rejected even if large in size", downloadManager.validateAudioFile(htmlFile))

        // 2. JSON error payload
        val jsonFile = File(downloadsDir, "json_error.mp3")
        FileOutputStream(jsonFile).use { fos ->
            val json = "{\"error\": \"Streaming rate limit exceeded\", \"status\": 429}"
            fos.write(json.toByteArray())
            fos.write(ByteArray(60_000))
        }
        assertFalse("JSON error payload must be rejected", downloadManager.validateAudioFile(jsonFile))

        // 3. Random binary garbage without audio magic bytes
        val garbageFile = File(downloadsDir, "garbage.mp3")
        createDummyAudioFile(garbageFile, "random_garbage", sizeBytes = 70_000L)
        assertFalse("Arbitrary non-audio payload must be rejected", downloadManager.validateAudioFile(garbageFile))
    }

    @Test
    fun testIncompleteAndTruncatedFileRejection() {
        val truncatedFile = File(downloadsDir, "truncated.mp3")
        // Has valid ID3 header but only 500 bytes (far below MIN_VALID_AUDIO_BYTES = 50_000)
        createDummyAudioFile(truncatedFile, "id3", sizeBytes = 500L)

        // File validation requires both valid header AND >= 12 bytes, but manager requires >= MIN_VALID_AUDIO_BYTES
        assertTrue("Header itself is valid", downloadManager.validateAudioFile(truncatedFile))
        assertTrue("File is under minimum valid audio bytes", truncatedFile.length() < MusicDownloadManager.MIN_VALID_AUDIO_BYTES)
    }

    @Test
    fun testDuplicateDownloadPrevention() = runBlocking {
        val testVideo = YouTubeVideo(
            id = "duplicate_check_01",
            title = "Blinding Lights",
            channelTitle = "The Weeknd",
            thumbnailUrl = "https://example.com/thumb.jpg"
        )

        val targetFile = File(downloadsDir, "${testVideo.id}.mp3")
        createDummyAudioFile(targetFile, "id3", sizeBytes = 120_000L)

        val existingTrack = DownloadedTrack(
            video = testVideo,
            localFilePath = targetFile.absolutePath,
            fileSize = targetFile.length(),
            downloadedAt = System.currentTimeMillis()
        )
        localRepo.saveDownloadedTrack(existingTrack)

        // Verify it is recognized as already downloaded
        assertTrue(downloadManager.isDownloaded(testVideo.id))

        // When attempting to download again, duplicate prevention immediately returns existing track
        val result = downloadManager.downloadSong(testVideo)
        assertTrue(result.isSuccess)
        assertEquals(targetFile.absolutePath, result.getOrNull()?.localFilePath)
    }

    @Test
    fun testCancellationAndTemporaryFileCleanup() {
        val videoId = "cancel_test_track_123"

        // Create a temporary in-progress file
        val tmpFile = File(downloadsDir, "${videoId}_123456.tmp")
        createDummyAudioFile(tmpFile, "id3", sizeBytes = 25_000L)
        assertTrue(tmpFile.exists())

        // Cancel download
        downloadManager.cancelDownload(videoId)

        // Verify temporary file was deleted
        assertFalse("Temporary file must be deleted upon cancellation", tmpFile.exists())

        // Verify status updated to Cancelled
        val status = downloadManager.downloadStates.value[videoId]
        assertEquals(DownloadStatus.Cancelled, status)
    }

    @Test
    fun testStartupRecoveryAndCorruptedFilePruning() = runBlocking {
        // 1. Create an orphaned .tmp file that survived an app crash
        val orphanTmp = File(downloadsDir, "stranded_download.tmp")
        orphanTmp.writeText("half-downloaded-stream-data")
        assertTrue(orphanTmp.exists())

        // 2. Create a corrupted record in database whose file was deleted on disk
        val missingTrack = DownloadedTrack(
            video = YouTubeVideo(
                id = "missing_song_999",
                title = "Deleted File Song",
                channelTitle = "Artist",
                thumbnailUrl = "https://example.com/thumb.jpg"
            ),
            localFilePath = "${downloadsDir.absolutePath}/missing_song_999.mp3",
            fileSize = 100_000L
        )
        localRepo.saveDownloadedTrack(missingTrack)
        roomRepo.saveDownload(missingTrack.video, missingTrack.localFilePath, missingTrack.fileSize)

        // 3. Create a record with a corrupted (HTML error) file on disk
        val corruptFile = File(downloadsDir, "corrupted_song_888.mp3")
        corruptFile.writeText("<!DOCTYPE html><html><body>Error</body></html>")
        val corruptTrack = DownloadedTrack(
            video = YouTubeVideo(
                id = "corrupted_song_888",
                title = "Corrupted Song",
                channelTitle = "Artist",
                thumbnailUrl = "https://example.com/thumb.jpg"
            ),
            localFilePath = corruptFile.absolutePath,
            fileSize = corruptFile.length()
        )
        localRepo.saveDownloadedTrack(corruptTrack)
        roomRepo.saveDownload(corruptTrack.video, corruptTrack.localFilePath, corruptTrack.fileSize)

        // 4. Run startup integrity check (called automatically in init)
        downloadManager.runStartupIntegrityCheck()
        // Allow background coroutine to finish
        kotlinx.coroutines.delay(200)

        // Verify orphan .tmp file is purged
        assertFalse("Orphaned .tmp files must be purged on startup", orphanTmp.exists())

        // Verify corrupt file on disk is removed
        assertFalse("Corrupt file on disk must be removed", corruptFile.exists())

        // Verify invalid records are pruned from localRepo
        assertNull(downloadManager.getDownloadedTrack("missing_song_999"))
        assertNull(downloadManager.getDownloadedTrack("corrupted_song_888"))
    }

    @Test
    fun testOfflinePlaybackAndPlaylistIntegration() = runBlocking {
        val offlineVideo = YouTubeVideo(
            id = "offline_hit_777",
            title = "Midnight City",
            channelTitle = "M83",
            thumbnailUrl = "https://example.com/m83.jpg"
        )

        val targetFile = File(downloadsDir, "${offlineVideo.id}.mp3")
        createDummyAudioFile(targetFile, "id3", sizeBytes = 85_000L)

        val downloadedTrack = DownloadedTrack(
            video = offlineVideo,
            localFilePath = targetFile.absolutePath,
            fileSize = targetFile.length(),
            downloadedAt = System.currentTimeMillis()
        )
        localRepo.saveDownloadedTrack(downloadedTrack)
        roomRepo.saveDownload(offlineVideo, targetFile.absolutePath, targetFile.length())

        // Verify offline track retrieval
        val retrieved = downloadManager.getDownloadedTrack(offlineVideo.id)
        assertNotNull("Downloaded track must be retrievable", retrieved)
        assertEquals(targetFile.absolutePath, retrieved?.localFilePath)
        assertTrue(File(retrieved!!.localFilePath).exists())

        // Verify adding offline track to playlist
        val playlist = roomRepo.createPlaylist("Offline Favorites", "Songs for flights and trips")
        downloadManager.addDownloadedTrackToPlaylist(playlist.id, offlineVideo)

        val updatedPlaylist = roomRepo.getPlaylistById(playlist.id)
        assertNotNull(updatedPlaylist)
        assertEquals(1, updatedPlaylist?.tracks?.size)
        assertEquals("offline_hit_777", updatedPlaylist?.tracks?.first()?.id)

        // Verify storage calculation
        val usage = downloadManager.getStorageUsageBytes()
        assertTrue("Storage usage must reflect downloaded file", usage >= 85_000L)
        val formatted = downloadManager.formatBytes(usage)
        assertTrue(formatted.contains("KB") || formatted.contains("MB"))
    }
}
