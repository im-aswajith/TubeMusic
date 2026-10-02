package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.YouTubeVideo

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val channelTitle: String,
    val channelThumbnailUrl: String? = null,
    val thumbnailUrl: String,
    val duration: String? = null,
    val viewCountText: String? = null,
    val publishedTimeText: String? = null,
    val isLive: Boolean = false,
    val description: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toVideo(): YouTubeVideo = YouTubeVideo(
        id = id,
        title = title,
        channelTitle = channelTitle,
        channelThumbnailUrl = channelThumbnailUrl,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        viewCountText = viewCountText,
        publishedTimeText = publishedTimeText,
        isLive = isLive,
        description = description
    )

    companion object {
        fun fromVideo(v: YouTubeVideo, timestamp: Long = System.currentTimeMillis()): TrackEntity = TrackEntity(
            id = v.id,
            title = v.title,
            channelTitle = v.channelTitle,
            channelThumbnailUrl = v.channelThumbnailUrl,
            thumbnailUrl = v.thumbnailUrl,
            duration = v.duration,
            viewCountText = v.viewCountText,
            publishedTimeText = v.publishedTimeText,
            isLive = v.isLive,
            description = v.description,
            addedAt = timestamp
        )
    }
}

@Entity(tableName = "liked_songs")
data class LikedSongEntity(
    @PrimaryKey val videoId: String,
    val likedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val videoId: String,
    val playedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val coverUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "videoId"]
)
data class PlaylistTrackCrossRef(
    val playlistId: String,
    val videoId: String,
    val sortOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val videoId: String,
    val localFilePath: String,
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
    val audioQuality: String = "High (320 kbps)",
    val status: String = "COMPLETED" // QUEUED, DOWNLOADING, COMPLETED, FAILED
)

@Entity(tableName = "listening_stats")
data class ListeningStatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val videoId: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis()
)
