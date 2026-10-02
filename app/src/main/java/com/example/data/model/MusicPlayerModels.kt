package com.example.data.model

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class PlayerDisplayMode {
    AUDIO_ARTWORK,
    VIDEO_STREAM
}

enum class EqualizerPreset(val label: String, val description: String) {
    NORMAL("Normal", "Balanced, studio-grade sound reproduction"),
    BASS_BOOST("Bass Boost", "Deep enhanced low-end frequencies for punchy beats"),
    VOCAL_BOOST("Vocal Boost", "Crisp midrange clarity for vocals and podcasts"),
    ACOUSTIC("Acoustic", "Warm acoustic tone with open acoustic treble"),
    EDM_CLUB("EDM & Club", "High dynamic range with elevated bass and highs"),
    ROCK("Rock", "Aggressive guitars with punchy drums and bass"),
    LOFI_CHILL("Lo-Fi Chill", "Smooth vintage tape warmth with soft roll-off")
}

data class Playlist(
    val id: String,
    val title: String,
    val description: String = "",
    val coverUrl: String? = null,
    val videoIds: List<String> = emptyList(),
    val tracks: List<YouTubeVideo> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class ActivityType {
    PLAYED_SONG,
    LIKED_SONG,
    UNLIKED_SONG,
    CREATED_PLAYLIST,
    DELETED_PLAYLIST,
    ADDED_TO_PLAYLIST,
    REMOVED_FROM_PLAYLIST
}

data class ActivityEvent(
    val id: String,
    val type: ActivityType,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val trackId: String? = null,
    val playlistId: String? = null
)

data class DownloadedTrack(
    val video: YouTubeVideo,
    val localFilePath: String,
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
    val audioQuality: String = "High (320 kbps)"
) {
    val formattedSize: String
        get() {
            val mb = fileSize.toDouble() / (1024 * 1024)
            return if (mb >= 0.1) String.format(java.util.Locale.US, "%.1f MB", mb)
            else String.format(java.util.Locale.US, "%d KB", (fileSize / 1024).coerceAtLeast(1))
        }
}

enum class DownloadState {
    IDLE,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data object Queued : DownloadStatus()
    data class Downloading(
        val progress: Float,
        val bytesDownloaded: Long = 0L,
        val totalBytes: Long = 0L
    ) : DownloadStatus()
    data class Completed(val track: DownloadedTrack) : DownloadStatus()
    data class Failed(val error: String, val canRetry: Boolean = true) : DownloadStatus()
    data object Cancelled : DownloadStatus()
}
