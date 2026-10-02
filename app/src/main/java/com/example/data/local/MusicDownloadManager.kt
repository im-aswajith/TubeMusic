package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadedTrack
import com.example.data.model.YouTubeVideo
import com.example.data.repository.RoomMusicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MusicDownloadManager(
    private val context: Context,
    private val repository: LocalPreferencesRepository = LocalPreferencesRepository(context),
    private val roomRepository: RoomMusicRepository = RoomMusicRepository(context)
) {
    companion object {
        private const val TAG = "MusicDownloadManager"
        private const val DOWNLOADS_DIR = "music_downloads"
        const val MIN_VALID_AUDIO_BYTES = 50_000L // Minimum 50KB for a genuine music track

        private val STREAM_INSTANCES = listOf(
            "https://api.piped.privacydev.net",
            "https://pipedapi.kavin.rocks",
            "https://inv.nadeko.net",
            "https://invidious.nerdvpn.de",
            "https://iv.melmac.space",
            "https://invidious.jing.rocks"
        )
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val downloadsDir: File
        get() = File(context.filesDir, DOWNLOADS_DIR).apply { if (!exists()) mkdirs() }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Detailed download status tracking
    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    // Backward-compatible progress & active download flows
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Set<String>>(emptySet())
    val activeDownloads: StateFlow<Set<String>> = _activeDownloads.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    init {
        runStartupIntegrityCheck()
    }

    /**
     * Recovery and cleanup after app restart or process recreation:
     * 1. Prunes leftover orphaned temporary files (*.tmp)
     * 2. Validates all existing records against physical files on disk
     * 3. Purges corrupted or zero-byte files from storage and database
     */
    fun runStartupIntegrityCheck() {
        scope.launch {
            try {
                // 1. Clean up stale .tmp files
                downloadsDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".tmp")) {
                        Log.d(TAG, "Pruning orphaned temporary download file: ${file.name}")
                        file.delete()
                    }
                }

                // 2. Validate stored downloads in local preferences and database
                val stored = repository.getDownloadedTracks()
                val validTracks = mutableListOf<DownloadedTrack>()

                for (track in stored) {
                    val file = File(track.localFilePath)
                    if (file.exists() && validateAudioFile(file)) {
                        validTracks.add(track)
                    } else {
                        Log.w(TAG, "Removing corrupted or missing download record: ${track.video.id}")
                        if (file.exists()) file.delete()
                        roomRepository.removeDownload(track.video.id)
                    }
                }

                // Save back pruned list if any entries were purged
                if (validTracks.size != stored.size) {
                    repository.saveDownloadedTracksList(validTracks)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error during download manager startup integrity check", e)
            }
        }
    }

    fun isDownloaded(videoId: String): Boolean {
        val dt = repository.getDownloadedTracks().firstOrNull { it.video.id == videoId }
        if (dt != null) {
            val file = File(dt.localFilePath)
            if (file.exists() && validateAudioFile(file)) return true
        }
        val targetMp3 = File(downloadsDir, "$videoId.mp3")
        if (targetMp3.exists() && validateAudioFile(targetMp3)) return true
        val targetM4a = File(downloadsDir, "$videoId.m4a")
        if (targetM4a.exists() && validateAudioFile(targetM4a)) return true
        val targetWebm = File(downloadsDir, "$videoId.webm")
        return targetWebm.exists() && validateAudioFile(targetWebm)
    }

    fun getDownloadedTrack(videoId: String): DownloadedTrack? {
        val track = repository.getDownloadedTracks().firstOrNull { it.video.id == videoId } ?: return null
        val file = File(track.localFilePath)
        return if (file.exists() && validateAudioFile(file)) track else null
    }

    fun getAllDownloadedTracks(): List<DownloadedTrack> {
        val tracks = repository.getDownloadedTracks()
        return tracks.filter { dt ->
            val file = File(dt.localFilePath)
            file.exists() && validateAudioFile(file)
        }
    }

    fun getAllDownloadedTracksAsVideos(): List<YouTubeVideo> {
        return getAllDownloadedTracks().map { it.video }
    }

    fun isDownloading(videoId: String): Boolean {
        return activeJobs.containsKey(videoId) || _activeDownloads.value.contains(videoId)
    }

    fun cancelDownload(videoId: String) {
        val job = activeJobs.remove(videoId)
        job?.cancel(CancellationException("Download cancelled by user"))

        _activeDownloads.value = _activeDownloads.value - videoId
        _downloadProgress.value = _downloadProgress.value - videoId
        _downloadStates.value = _downloadStates.value + (videoId to DownloadStatus.Cancelled)

        // Delete any temporary files for this video
        downloadsDir.listFiles()?.forEach { file ->
            if (file.name.startsWith(videoId) && file.name.endsWith(".tmp")) {
                file.delete()
            }
        }
    }

    suspend fun retryDownload(
        video: YouTubeVideo,
        quality: String = repository.getDownloadQuality(),
        onProgressUpdate: ((Float) -> Unit)? = null
    ): Result<DownloadedTrack> {
        cancelDownload(video.id)
        _downloadStates.value = _downloadStates.value - video.id
        return downloadSong(video, quality, onProgressUpdate)
    }

    suspend fun downloadSong(
        video: YouTubeVideo,
        quality: String = repository.getDownloadQuality(),
        onProgressUpdate: ((Float) -> Unit)? = null
    ): Result<DownloadedTrack> = withContext(Dispatchers.IO) {
        val videoId = video.id
        if (videoId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid video id"))
        }

        // Duplicate prevention: If already downloading, do not spawn duplicate tasks
        if (isDownloading(videoId)) {
            return@withContext Result.failure(IllegalStateException("Download already in progress for track '${video.title}'"))
        }

        // Check if already downloaded with valid audio file
        if (isDownloaded(videoId)) {
            val existing = getDownloadedTrack(videoId)
            if (existing != null) {
                _downloadStates.value = _downloadStates.value + (videoId to DownloadStatus.Completed(existing))
                return@withContext Result.success(existing)
            }
        }

        val currentJob = coroutineContext[Job]
        if (currentJob != null) {
            activeJobs[videoId] = currentJob
        }

        // Update states to Queued / Starting
        _activeDownloads.value = _activeDownloads.value + videoId
        _downloadProgress.value = _downloadProgress.value + (videoId to 0.05f)
        _downloadStates.value = _downloadStates.value + (videoId to DownloadStatus.Queued)
        onProgressUpdate?.invoke(0.05f)

        val tmpFile = File(downloadsDir, "${videoId}_${System.currentTimeMillis()}.tmp")
        val thumbFile = File(downloadsDir, "${videoId}_thumb.jpg")

        try {
            if (tmpFile.exists()) tmpFile.delete()

            // 1. Download offline artwork
            downloadThumbnail(video.thumbnailUrl, thumbFile)
            _downloadProgress.value = _downloadProgress.value + (videoId to 0.12f)
            _downloadStates.value = _downloadStates.value + (videoId to DownloadStatus.Downloading(0.12f, 0L, 0L))
            onProgressUpdate?.invoke(0.12f)

            // 2. Resolve audio stream from open API instances
            val streamInfo = resolveAudioStreamUrl(videoId)
                ?: throw IllegalStateException("Audio stream for '${video.title}' is currently restricted or unavailable for offline download.")

            val resolvedFormat = streamInfo.format

            // 3. Download genuine audio stream
            val downloadSuccess = downloadStreamToFile(streamInfo.url, tmpFile) { progress, bytesRead, totalBytes ->
                val scaled = 0.15f + (progress * 0.80f)
                _downloadProgress.value = _downloadProgress.value + (videoId to scaled)
                _downloadStates.value = _downloadStates.value + (videoId to DownloadStatus.Downloading(scaled, bytesRead, totalBytes))
                onProgressUpdate?.invoke(scaled)
            }

            // 4. Strict Validation: Reject fake, failed, or incomplete downloads
            if (!downloadSuccess || !tmpFile.exists() || tmpFile.length() < MIN_VALID_AUDIO_BYTES) {
                if (tmpFile.exists()) tmpFile.delete()
                throw IllegalStateException("Audio stream download failed or returned incomplete payload for '${video.title}'.")
            }

            // 5. Verify audio magic bytes (MP3, M4A, WebM/Opus, OGG, FLAC, WAV, AAC)
            if (!validateAudioFile(tmpFile)) {
                tmpFile.delete()
                throw IllegalStateException("Downloaded file failed audio validation: corrupted or invalid audio header.")
            }

            // 6. Atomic move to target destination
            val targetFile = File(downloadsDir, "$videoId.$resolvedFormat")
            if (targetFile.exists()) targetFile.delete()

            val renamed = tmpFile.renameTo(targetFile)
            if (!renamed) {
                tmpFile.copyTo(targetFile, overwrite = true)
                tmpFile.delete()
            }

            val finalThumbUrl = if (thumbFile.exists()) "file://${thumbFile.absolutePath}" else video.thumbnailUrl
            val downloadedTrack = DownloadedTrack(
                video = video.copy(thumbnailUrl = finalThumbUrl),
                localFilePath = targetFile.absolutePath,
                fileSize = targetFile.length(),
                downloadedAt = System.currentTimeMillis(),
                audioQuality = quality
            )

            // 7. Persist metadata in both Room database and local preferences
            repository.saveDownloadedTrack(downloadedTrack)
            roomRepository.saveDownload(
                video = downloadedTrack.video,
                localFilePath = downloadedTrack.localFilePath,
                fileSize = downloadedTrack.fileSize,
                quality = quality
            )

            _downloadProgress.value = _downloadProgress.value + (videoId to 1.0f)
            _downloadStates.value = _downloadStates.value + (videoId to DownloadStatus.Completed(downloadedTrack))
            onProgressUpdate?.invoke(1.0f)

            Result.success(downloadedTrack)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $videoId: ${e.message}")
            if (tmpFile.exists()) tmpFile.delete()

            val isCancelled = e is CancellationException
            val errorMsg = if (isCancelled) "Download cancelled" else (e.message ?: "Download failed")
            val status = if (isCancelled) DownloadStatus.Cancelled else DownloadStatus.Failed(errorMsg, canRetry = true)
            _downloadStates.value = _downloadStates.value + (videoId to status)

            Result.failure(e)
        } finally {
            _activeDownloads.value = _activeDownloads.value - videoId
            _downloadProgress.value = _downloadProgress.value - videoId
            activeJobs.remove(videoId)
        }
    }

    private data class StreamInfo(val url: String, val format: String)

    private suspend fun resolveAudioStreamUrl(videoId: String): StreamInfo? {
        for (instance in STREAM_INSTANCES) {
            try {
                val url = "$instance/streams/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    val audioStreams = json.optJSONArray("audioStreams")
                    if (audioStreams != null && audioStreams.length() > 0) {
                        var bestUrl: String? = null
                        var bestBitrate = 0
                        var format = "m4a"

                        for (i in 0 until audioStreams.length()) {
                            val stream = audioStreams.getJSONObject(i)
                            val bitrate = stream.optInt("bitrate", 0)
                            val sUrl = stream.optString("url")
                            val mimeType = stream.optString("mimeType", "")
                            if (sUrl.isNotBlank() && bitrate >= bestBitrate) {
                                bestBitrate = bitrate
                                bestUrl = sUrl
                                format = when {
                                    mimeType.contains("mp4") || mimeType.contains("m4a") -> "m4a"
                                    mimeType.contains("webm") || mimeType.contains("opus") -> "webm"
                                    mimeType.contains("ogg") -> "ogg"
                                    else -> "mp3"
                                }
                            }
                        }
                        if (bestUrl != null) return StreamInfo(bestUrl, format)
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun downloadStreamToFile(
        url: String,
        destFile: File,
        onProgress: (Float, Long, Long) -> Unit
    ): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false

                // Reject non-audio payloads (e.g. HTML error pages)
                val contentType = response.header("Content-Type")?.lowercase() ?: ""
                if (contentType.contains("text/html") || contentType.contains("application/json")) {
                    Log.w(TAG, "Rejected download response with invalid content-type: $contentType")
                    return false
                }

                val contentLength = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            val progress = if (contentLength > 0) {
                                (totalRead.toFloat() / contentLength).coerceIn(0f, 1f)
                            } else {
                                (totalRead.toFloat() / 5_000_000f).coerceIn(0f, 0.95f)
                            }
                            onProgress(progress, totalRead, contentLength)
                        }
                        output.flush()
                    }
                }
                destFile.exists() && destFile.length() >= MIN_VALID_AUDIO_BYTES
            }
        } catch (e: Exception) {
            Log.w(TAG, "Download stream failed: ${e.message}")
            false
        }
    }

    private suspend fun downloadThumbnail(url: String, destFile: File) {
        if (url.isBlank()) return
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Validates audio format magic bytes:
     * - ID3v2: "ID3"
     * - MP3 sync frame: 0xFF followed by 0xFB, 0xF3, 0xF2, 0xE0..
     * - MP4 / M4A: "ftyp" at offset 4..7
     * - WebM / Matroska (Opus/Vorbis): 0x1A 0x45 0xDF 0xA3
     * - OGG: "OggS"
     * - FLAC: "fLaC"
     * - RIFF / WAVE: "RIFF" and "WAVE"
     * - AAC ADTS: 0xFF 0xF0..0xFF
     * Rejects HTML, JSON, and arbitrary non-audio text files.
     */
    fun validateAudioFile(file: File): Boolean {
        if (!file.exists() || file.length() < 12) return false
        try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(16)
                val read = fis.read(header)
                if (read < 12) return false

                // Reject clear HTML/XML/JSON text responses
                val firstAscii = String(header, 0, minOf(read, 8), Charsets.US_ASCII).lowercase()
                if (firstAscii.startsWith("<!do") || firstAscii.startsWith("<htm") ||
                    firstAscii.startsWith("<?xm") || firstAscii.startsWith("{\"") ||
                    firstAscii.startsWith("error")
                ) {
                    return false
                }

                // 1. ID3v2 header: "ID3" (0x49 0x44 0x33)
                if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
                    return true
                }

                // 2. MP3 frame sync: 0xFF followed by 0xE0..0xFF
                val b0 = header[0].toInt() and 0xFF
                val b1 = header[1].toInt() and 0xFF
                if (b0 == 0xFF && (b1 and 0xE0) == 0xE0) {
                    return true
                }

                // 3. MP4 / M4A: bytes 4..7 are "ftyp"
                if (header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() &&
                    header[6] == 'y'.code.toByte() && header[7] == 'p'.code.toByte()
                ) {
                    return true
                }

                // 4. WebM / Matroska (EBML: 0x1A 0x45 0xDF 0xA3)
                if (b0 == 0x1A && b1 == 0x45 && (header[2].toInt() and 0xFF) == 0xDF && (header[3].toInt() and 0xFF) == 0xA3) {
                    return true
                }

                // 5. OGG Vorbis / Opus: "OggS"
                if (header[0] == 'O'.code.toByte() && header[1] == 'g'.code.toByte() &&
                    header[2] == 'g'.code.toByte() && header[3] == 'S'.code.toByte()
                ) {
                    return true
                }

                // 6. FLAC: "fLaC"
                if (header[0] == 'f'.code.toByte() && header[1] == 'L'.code.toByte() &&
                    header[2] == 'a'.code.toByte() && header[3] == 'C'.code.toByte()
                ) {
                    return true
                }

                // 7. RIFF / WAVE
                if (header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
                    read >= 12 &&
                    header[8] == 'W'.code.toByte() && header[9] == 'A'.code.toByte() &&
                    header[10] == 'V'.code.toByte() && header[11] == 'E'.code.toByte()
                ) {
                    return true
                }

                // 8. AAC ADTS: 0xFF and (b1 & 0xF6) == 0xF0
                if (b0 == 0xFF && (b1 and 0xF6) == 0xF0) {
                    return true
                }
            }
        } catch (_: Exception) {
            return false
        }
        return false
    }

    suspend fun deleteDownloadedTrack(videoId: String): Boolean = deleteDownloadedSong(videoId)

    suspend fun deleteDownloadedSong(videoId: String): Boolean = withContext(Dispatchers.IO) {
        val dt = repository.getDownloadedTracks().firstOrNull { it.video.id == videoId }
        var deleted = false
        if (dt != null) {
            val file = File(dt.localFilePath)
            if (file.exists()) {
                deleted = file.delete()
            }
        }
        val mp3 = File(downloadsDir, "$videoId.mp3")
        if (mp3.exists()) deleted = mp3.delete() || deleted
        val m4a = File(downloadsDir, "$videoId.m4a")
        if (m4a.exists()) deleted = m4a.delete() || deleted
        val webm = File(downloadsDir, "$videoId.webm")
        if (webm.exists()) deleted = webm.delete() || deleted
        val thumb = File(downloadsDir, "${videoId}_thumb.jpg")
        if (thumb.exists()) thumb.delete()

        repository.removeDownloadedTrack(videoId)
        roomRepository.removeDownload(videoId)
        _downloadStates.value = _downloadStates.value - videoId
        deleted
    }

    suspend fun addDownloadedTrackToPlaylist(playlistId: String, video: YouTubeVideo) {
        if (isDownloaded(video.id)) {
            repository.addTrackToPlaylist(playlistId, video)
            roomRepository.addTrackToPlaylist(playlistId, video)
        }
    }

    fun getStorageUsageBytes(): Long {
        var total = 0L
        downloadsDir.listFiles()?.forEach { file ->
            total += file.length()
        }
        return total
    }

    fun getDownloadedAudioSizeBytes(): Long = getStorageUsageBytes()

    fun getCacheSizeBytes(): Long {
        var size = 0L
        try {
            context.cacheDir.walkTopDown().forEach { size += it.length() }
        } catch (_: Exception) {}
        return size
    }

    suspend fun clearAppCache(): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                freed += file.length()
                file.deleteRecursively()
            }
        } catch (_: Exception) {}
        freed
    }

    suspend fun clearAllDownloads(): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        downloadsDir.listFiles()?.forEach {
            freed += it.length()
            it.delete()
        }
        repository.clearAllDownloadedTracks()
        roomRepository.clearAllDownloads()
        _downloadStates.value = emptyMap()
        freed
    }

    fun formatBytes(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return if (mb >= 0.1) String.format(java.util.Locale.US, "%.1f MB", mb)
        else String.format(java.util.Locale.US, "%d KB", (bytes / 1024).coerceAtLeast(0))
    }
}
