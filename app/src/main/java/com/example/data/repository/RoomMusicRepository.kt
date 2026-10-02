package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.LocalPreferencesRepository
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.DownloadEntity
import com.example.data.local.db.LikedSongEntity
import com.example.data.local.db.ListeningStatEntity
import com.example.data.local.db.PlaylistEntity
import com.example.data.local.db.PlaylistTrackCrossRef
import com.example.data.local.db.RecentlyPlayedEntity
import com.example.data.local.db.TopArtistStat
import com.example.data.local.db.TopTrackStat
import com.example.data.local.db.TrackEntity
import com.example.data.model.DownloadedTrack
import com.example.data.model.Playlist
import com.example.data.model.YouTubeVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class RoomMusicRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context)
) {
    private val trackDao = database.trackDao()
    private val likedDao = database.likedSongDao()
    private val recentDao = database.recentlyPlayedDao()
    private val playlistDao = database.playlistDao()
    private val downloadDao = database.downloadDao()
    private val statsDao = database.listeningStatDao()

    companion object {
        private const val TAG = "RoomMusicRepository"
    }

    // Liked songs
    val likedTracksFlow: Flow<List<YouTubeVideo>> = likedDao.getLikedTracksFlow()
        .map { list -> list.map { it.toVideo() } }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    val likedCountFlow: Flow<Int> = likedDao.getLikedCountFlow()
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    suspend fun isTrackLiked(videoId: String): Boolean = withContext(Dispatchers.IO) {
        likedDao.isLiked(videoId)
    }

    fun isTrackLikedFlow(videoId: String): Flow<Boolean> = likedDao.isLikedFlow(videoId)
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    suspend fun toggleLikedTrack(video: YouTubeVideo): Boolean = withContext(Dispatchers.IO) {
        trackDao.insertTrack(TrackEntity.fromVideo(video))
        val isAlreadyLiked = likedDao.isLiked(video.id)
        if (isAlreadyLiked) {
            likedDao.deleteLiked(video.id)
            false
        } else {
            likedDao.insertLiked(LikedSongEntity(videoId = video.id))
            true
        }
    }

    // Recently played
    val recentlyPlayedFlow: Flow<List<YouTubeVideo>> = recentDao.getRecentlyPlayedTracksFlow(50)
        .map { list -> list.map { it.toVideo() } }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    suspend fun addRecentlyPlayed(video: YouTubeVideo) = withContext(Dispatchers.IO) {
        trackDao.insertTrack(TrackEntity.fromVideo(video))
        recentDao.insertRecent(
            RecentlyPlayedEntity(
                videoId = video.id,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearRecentlyPlayed() = withContext(Dispatchers.IO) {
        recentDao.clearAll()
    }

    // Playlists
    val playlistsFlow: Flow<List<Playlist>> = playlistDao.getAllPlaylistsFlow()
        .map { list ->
            list.map { entity ->
                val trackEntities = playlistDao.getTracksForPlaylist(entity.id)
                val tracks = trackEntities.map { it.toVideo() }
                val videoIds = tracks.map { it.id }
                Playlist(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    coverUrl = entity.coverUrl ?: tracks.firstOrNull()?.thumbnailUrl,
                    videoIds = videoIds,
                    tracks = tracks,
                    createdAt = entity.createdAt
                )
            }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    suspend fun getPlaylistById(id: String): Playlist? = withContext(Dispatchers.IO) {
        val entity = playlistDao.getPlaylistById(id) ?: return@withContext null
        val trackEntities = playlistDao.getTracksForPlaylist(id)
        val tracks = trackEntities.map { it.toVideo() }
        Playlist(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            coverUrl = entity.coverUrl ?: tracks.firstOrNull()?.thumbnailUrl,
            videoIds = tracks.map { it.id },
            tracks = tracks,
            createdAt = entity.createdAt
        )
    }

    suspend fun createPlaylist(title: String, description: String = "", coverUrl: String? = null): Playlist = withContext(Dispatchers.IO) {
        val id = "pl_${System.currentTimeMillis()}"
        val entity = PlaylistEntity(
            id = id,
            title = title.trim().ifBlank { "New Playlist" },
            description = description.trim(),
            coverUrl = coverUrl,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(entity)
        Playlist(
            id = id,
            title = entity.title,
            description = entity.description,
            coverUrl = entity.coverUrl,
            videoIds = emptyList(),
            tracks = emptyList(),
            createdAt = entity.createdAt
        )
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistById(playlistId)
        playlistDao.clearTracksForPlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: String, video: YouTubeVideo) = withContext(Dispatchers.IO) {
        trackDao.insertTrack(TrackEntity.fromVideo(video))
        val currentIds = playlistDao.getVideoIdsForPlaylist(playlistId)
        if (!currentIds.contains(video.id)) {
            playlistDao.insertPlaylistTrack(
                PlaylistTrackCrossRef(
                    playlistId = playlistId,
                    videoId = video.id,
                    sortOrder = currentIds.size,
                    addedAt = System.currentTimeMillis()
                )
            )
            // Update playlist updatedAt and auto-cover if empty
            val pl = playlistDao.getPlaylistById(playlistId)
            if (pl != null) {
                val newCover = if (pl.coverUrl.isNullOrBlank()) video.thumbnailUrl else pl.coverUrl
                playlistDao.insertPlaylist(pl.copy(updatedAt = System.currentTimeMillis(), coverUrl = newCover))
            }
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, videoId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, videoId)
        val pl = playlistDao.getPlaylistById(playlistId)
        if (pl != null) {
            playlistDao.insertPlaylist(pl.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    // Downloads
    suspend fun saveDownload(
        video: YouTubeVideo,
        localFilePath: String,
        fileSize: Long,
        quality: String = "High (320 kbps)"
    ) = withContext(Dispatchers.IO) {
        trackDao.insertTrack(TrackEntity.fromVideo(video))
        downloadDao.insertDownload(
            DownloadEntity(
                videoId = video.id,
                localFilePath = localFilePath,
                fileSize = fileSize,
                downloadedAt = System.currentTimeMillis(),
                audioQuality = quality,
                status = "COMPLETED"
            )
        )
    }

    suspend fun removeDownload(videoId: String) = withContext(Dispatchers.IO) {
        val download = downloadDao.getDownloadById(videoId)
        if (download != null) {
            val file = File(download.localFilePath)
            if (file.exists()) {
                file.delete()
            }
            downloadDao.deleteDownload(videoId)
        }
    }

    suspend fun clearAllDownloads() = withContext(Dispatchers.IO) {
        downloadDao.clearAll()
    }

    suspend fun isDownloaded(videoId: String): Boolean = withContext(Dispatchers.IO) {
        val download = downloadDao.getDownloadById(videoId) ?: return@withContext false
        val file = File(download.localFilePath)
        file.exists() && file.length() > 0
    }

    val downloadsFlow: Flow<List<DownloadedTrack>> = downloadDao.getAllDownloadsFlow()
        .map { downloads ->
            downloads.mapNotNull { d ->
                val trackEntity = trackDao.getTrackById(d.videoId) ?: return@mapNotNull null
                val file = File(d.localFilePath)
                if (file.exists() && file.length() > 0) {
                    DownloadedTrack(
                        video = trackEntity.toVideo(),
                        localFilePath = d.localFilePath,
                        fileSize = d.fileSize,
                        downloadedAt = d.downloadedAt,
                        audioQuality = d.audioQuality
                    )
                } else null
            }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    // Listening Stats
    suspend fun recordPlay(video: YouTubeVideo, durationPlayedSec: Long) = withContext(Dispatchers.IO) {
        if (durationPlayedSec < 5L) return@withContext // Ignore skip under 5 seconds
        trackDao.insertTrack(TrackEntity.fromVideo(video))
        statsDao.recordListeningEvent(
            ListeningStatEntity(
                videoId = video.id,
                title = video.title,
                artist = video.displayArtist,
                durationSeconds = durationPlayedSec,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    val totalListeningSecondsFlow: Flow<Long> = statsDao.getTotalListeningSecondsFlow()
        .map { it ?: 0L }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    val mostPlayedTracksFlow: Flow<List<TopTrackStat>> = statsDao.getMostPlayedFlow(10)
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    val topArtistsFlow: Flow<List<TopArtistStat>> = statsDao.getTopArtistsFlow(8)
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    val totalPlayCountFlow: Flow<Int> = statsDao.getTotalPlaysFlow()
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    // Search offline library
    suspend fun searchLocalTracks(query: String): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<YouTubeVideo>()
        // From liked
        val liked = likedDao.getLikedTracks()
        liked.filter { it.title.contains(query, ignoreCase = true) || it.channelTitle.contains(query, ignoreCase = true) }
            .forEach { results.add(it.toVideo()) }
        // From recents
        val recents = recentDao.getRecentlyPlayedTracks(50)
        recents.filter { it.title.contains(query, ignoreCase = true) || it.channelTitle.contains(query, ignoreCase = true) }
            .forEach { if (results.none { r -> r.id == it.id }) results.add(it.toVideo()) }
        results
    }

    // Versioned Backup and Restore (Supports Schema V2 and legacy V1 JSON format)
    suspend fun exportBackupJson(legacyRepo: LocalPreferencesRepository): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("schemaVersion", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "Ytmu")

        // 1. Liked Songs
        val likedEntities = likedDao.getLikedTracks()
        val likedArr = JSONArray()
        likedEntities.forEach { likedArr.put(videoEntityToJson(it)) }
        root.put("likedSongs", likedArr)

        // 2. Playlists
        val playlists = playlistDao.getAllPlaylists()
        val plArr = JSONArray()
        playlists.forEach { pl ->
            val pObj = JSONObject()
            pObj.put("id", pl.id)
            pObj.put("title", pl.title)
            pObj.put("description", pl.description)
            pObj.put("coverUrl", pl.coverUrl ?: "")
            pObj.put("createdAt", pl.createdAt)
            val tracks = playlistDao.getTracksForPlaylist(pl.id)
            val tArr = JSONArray()
            tracks.forEach { tArr.put(videoEntityToJson(it)) }
            pObj.put("tracks", tArr)
            plArr.put(pObj)
        }
        root.put("playlists", plArr)

        // 3. Settings
        val settings = JSONObject().apply {
            put("selectedTheme", legacyRepo.getSelectedTheme())
            put("streamingQuality", legacyRepo.getStreamingQuality())
            put("downloadQuality", legacyRepo.getDownloadQuality())
            put("userName", legacyRepo.getUserName())
            put("userEmail", legacyRepo.getUserEmail())
            put("repeatMode", legacyRepo.getRepeatMode().name)
            put("shuffle", legacyRepo.isShuffleEnabled())
            put("equalizerPreset", legacyRepo.getEqualizerPreset().name)
            put("bassBoost", legacyRepo.isBassBoost())
            put("virtualizer", legacyRepo.isVirtualizer())
        }
        root.put("settings", settings)

        root.toString(2)
    }

    suspend fun importBackupJson(
        jsonString: String,
        merge: Boolean = true,
        legacyRepo: LocalPreferencesRepository
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            var restoredPlaylists = 0
            var restoredLiked = 0

            // Playlists
            if (root.has("playlists")) {
                val plArr = root.getJSONArray("playlists")
                for (i in 0 until plArr.length()) {
                    val pObj = plArr.getJSONObject(i)
                    val id = pObj.optString("id", "pl_${System.currentTimeMillis()}_$i")
                    val title = pObj.optString("title", "Restored Playlist")
                    val desc = pObj.optString("description", "")
                    val cover = pObj.optString("coverUrl").ifBlank { null }
                    val createdAt = pObj.optLong("createdAt", System.currentTimeMillis())

                    playlistDao.insertPlaylist(
                        PlaylistEntity(
                            id = id,
                            title = title,
                            description = desc,
                            coverUrl = cover,
                            createdAt = createdAt,
                            updatedAt = createdAt
                        )
                    )

                    if (pObj.has("tracks")) {
                        val tArr = pObj.getJSONArray("tracks")
                        for (j in 0 until tArr.length()) {
                            val tObj = tArr.getJSONObject(j)
                            val tEntity = jsonToTrackEntity(tObj)
                            trackDao.insertTrack(tEntity)
                            playlistDao.insertPlaylistTrack(
                                PlaylistTrackCrossRef(
                                    playlistId = id,
                                    videoId = tEntity.id,
                                    sortOrder = j,
                                    addedAt = createdAt + j
                                )
                            )
                        }
                    }
                    restoredPlaylists++
                }
            }

            // Liked songs
            if (root.has("likedSongs")) {
                val likedArr = root.getJSONArray("likedSongs")
                for (i in 0 until likedArr.length()) {
                    val lObj = likedArr.getJSONObject(i)
                    val tEntity = jsonToTrackEntity(lObj)
                    trackDao.insertTrack(tEntity)
                    likedDao.insertLiked(LikedSongEntity(videoId = tEntity.id))
                    restoredLiked++
                }
            }

            // Settings
            if (root.has("settings")) {
                val s = root.getJSONObject("settings")
                if (s.has("selectedTheme")) legacyRepo.setSelectedTheme(s.getString("selectedTheme"))
                if (s.has("streamingQuality")) legacyRepo.setStreamingQuality(s.getString("streamingQuality"))
                if (s.has("downloadQuality")) legacyRepo.setDownloadQuality(s.getString("downloadQuality"))
                if (s.has("userName")) legacyRepo.setUserName(s.getString("userName"))
                if (s.has("userEmail")) legacyRepo.setUserEmail(s.getString("userEmail"))
            }

            ImportResult(
                success = true,
                playlistsCount = restoredPlaylists,
                likedCount = restoredLiked,
                message = "Restored $restoredPlaylists playlists and $restoredLiked liked songs"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import backup JSON", e)
            ImportResult(success = false, errorMessage = e.message ?: "Invalid backup file format")
        }
    }

    private fun videoEntityToJson(e: TrackEntity): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("title", e.title)
        put("channelTitle", e.channelTitle)
        put("channelThumbnailUrl", e.channelThumbnailUrl ?: "")
        put("thumbnailUrl", e.thumbnailUrl)
        put("duration", e.duration ?: "")
        put("viewCountText", e.viewCountText ?: "")
        put("publishedTimeText", e.publishedTimeText ?: "")
        put("isLive", e.isLive)
        put("description", e.description ?: "")
    }

    private fun jsonToTrackEntity(o: JSONObject): TrackEntity = TrackEntity(
        id = o.optString("id"),
        title = o.optString("title", "Untitled Track"),
        channelTitle = o.optString("channelTitle", "Artist"),
        channelThumbnailUrl = o.optString("channelThumbnailUrl").ifBlank { null },
        thumbnailUrl = o.optString("thumbnailUrl"),
        duration = o.optString("duration").ifBlank { null },
        viewCountText = o.optString("viewCountText").ifBlank { null },
        publishedTimeText = o.optString("publishedTimeText").ifBlank { null },
        isLive = o.optBoolean("isLive", false),
        description = o.optString("description").ifBlank { null }
    )
}

data class ImportResult(
    val success: Boolean = true,
    val playlistsCount: Int = 0,
    val likedCount: Int = 0,
    val message: String = "",
    val errorMessage: String? = null
)
