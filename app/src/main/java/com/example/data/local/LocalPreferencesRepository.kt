package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ActivityEvent
import com.example.data.model.ActivityType
import com.example.data.model.DownloadedTrack
import com.example.data.model.EqualizerPreset
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.YouTubeVideo
import org.json.JSONArray
import org.json.JSONObject

class LocalPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tubemusic_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val KEY_SAVED_VIDEOS = "saved_videos"
        private const val KEY_RECENTLY_PLAYED = "recently_played"
        private const val KEY_PLAYLISTS = "custom_playlists"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_SHUFFLE = "shuffle_enabled"
        private const val KEY_EQUALIZER = "equalizer_preset"
        private const val KEY_THEME = "selected_theme"
        private const val KEY_USER_NAME = "user_profile_name"
        private const val KEY_USER_EMAIL = "user_profile_email"
        private const val KEY_ACTIVITY_LOG = "activity_event_log"
        private const val KEY_DOWNLOADED_TRACKS = "downloaded_tracks_list"
        private const val KEY_DOWNLOAD_QUALITY = "download_quality_pref"
        private const val KEY_STREAMING_QUALITY = "streaming_quality_pref"
        private const val MAX_HISTORY = 20
        private const val MAX_RECENTS = 40
        private const val MAX_ACTIVITIES = 100
    }

    fun getSelectedTheme(): String {
        return prefs.getString(KEY_THEME, "Dark") ?: "Dark"
    }

    fun setSelectedTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "Music Listener") ?: "Music Listener"
    }

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun setUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getSearchHistory(): List<String> {
        val jsonStr = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val item = arr.getString(i)
                if (item.isNotBlank()) list.add(item)
            }
        } catch (_: Exception) {}
        return list
    }

    fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = getSearchHistory().toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val limited = current.take(MAX_HISTORY)

        val arr = JSONArray()
        limited.forEach { arr.put(it) }
        prefs.edit().putString(KEY_SEARCH_HISTORY, arr.toString()).apply()
    }

    fun removeSearchQuery(query: String) {
        val current = getSearchHistory().toMutableList()
        current.remove(query)
        val arr = JSONArray()
        current.forEach { arr.put(it) }
        prefs.edit().putString(KEY_SEARCH_HISTORY, arr.toString()).apply()
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    fun getSavedVideos(): List<YouTubeVideo> {
        val jsonStr = prefs.getString(KEY_SAVED_VIDEOS, null) ?: return emptyList()
        return parseVideosJson(jsonStr)
    }

    fun saveSavedVideos(videos: List<YouTubeVideo>) {
        saveVideosJson(KEY_SAVED_VIDEOS, videos)
    }

    fun toggleSavedVideo(video: YouTubeVideo): Boolean {
        val current = getSavedVideos().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == video.id }
        val isNowSaved = if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            false
        } else {
            current.add(0, video)
            true
        }
        saveVideosJson(KEY_SAVED_VIDEOS, current)
        return isNowSaved
    }

    fun isVideoSaved(videoId: String): Boolean {
        return getSavedVideos().any { it.id == videoId }
    }

    fun getRecentlyPlayed(): List<YouTubeVideo> {
        val jsonStr = prefs.getString(KEY_RECENTLY_PLAYED, null) ?: return emptyList()
        return parseVideosJson(jsonStr)
    }

    fun saveRecentlyPlayed(videos: List<YouTubeVideo>) {
        saveVideosJson(KEY_RECENTLY_PLAYED, videos)
    }

    fun addRecentlyPlayed(video: YouTubeVideo) {
        val current = getRecentlyPlayed().toMutableList()
        current.removeAll { it.id == video.id }
        current.add(0, video)
        val limited = current.take(MAX_RECENTS)
        saveVideosJson(KEY_RECENTLY_PLAYED, limited)
    }

    fun clearRecentlyPlayed() {
        prefs.edit().remove(KEY_RECENTLY_PLAYED).apply()
    }

    // Playlists Management
    fun getPlaylists(): List<Playlist> {
        val jsonStr = prefs.getString(KEY_PLAYLISTS, null) ?: return getDefaultPlaylists()
        val list = mutableListOf<Playlist>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val tracksArr = obj.optJSONArray("tracks")
                val tracks = mutableListOf<YouTubeVideo>()
                if (tracksArr != null) {
                    for (j in 0 until tracksArr.length()) {
                        val trackObj = tracksArr.getJSONObject(j)
                        tracks.add(parseSingleVideoJson(trackObj))
                    }
                }

                // If tracks are present, synchronize videoIds with tracks.
                // If tracks are empty, videoIds must also be empty so no phantom/unadded tracks appear.
                val videoIds = tracks.map { it.id }

                list.add(
                    Playlist(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        coverUrl = obj.optString("coverUrl").ifBlank { null },
                        videoIds = videoIds,
                        tracks = tracks,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return if (list.isEmpty()) getDefaultPlaylists() else list
    }

    private fun getDefaultPlaylists(): List<Playlist> {
        return listOf(
            Playlist(
                id = "pl_favorites",
                title = "Favorites & Liked Hits",
                description = "Your favorite music tracks and saved songs",
                coverUrl = null,
                videoIds = emptyList(),
                tracks = emptyList()
            ),
            Playlist(
                id = "pl_lofi",
                title = "Lo-Fi Study & Chill",
                description = "Mellow beats, ambient soundscapes, and relaxing vibes",
                coverUrl = null,
                videoIds = emptyList(),
                tracks = emptyList()
            ),
            Playlist(
                id = "pl_workout",
                title = "High Energy & Workout",
                description = "Bass heavy hype tracks and high-tempo bangers",
                coverUrl = null,
                videoIds = emptyList(),
                tracks = emptyList()
            )
        )
    }

    fun savePlaylist(playlist: Playlist) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlist.id }
        if (index >= 0) {
            current[index] = playlist
        } else {
            current.add(0, playlist)
        }
        persistPlaylists(current)
    }

    fun deletePlaylist(playlistId: String) {
        val current = getPlaylists().toMutableList()
        current.removeAll { it.id == playlistId }
        persistPlaylists(current)
    }

    fun addTrackToPlaylist(playlistId: String, video: YouTubeVideo) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val pl = current[index]
            val updatedTracks = pl.tracks.toMutableList()
            val thumb = if (video.thumbnailUrl.isNotBlank()) video.thumbnailUrl else "https://i.ytimg.com/vi/${video.id}/hqdefault.jpg"
            val sanitizedVideo = video.copy(thumbnailUrl = thumb)
            if (updatedTracks.none { it.id == video.id }) {
                updatedTracks.add(sanitizedVideo)
            }
            val updatedIds = updatedTracks.map { it.id }
            val cover = if (pl.coverUrl.isNullOrBlank() || pl.coverUrl.contains("unsplash")) thumb else pl.coverUrl
            current[index] = pl.copy(
                videoIds = updatedIds,
                tracks = updatedTracks,
                coverUrl = cover
            )
            persistPlaylists(current)
        }
    }

    fun addTrackToPlaylist(playlistId: String, videoId: String) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val pl = current[index]
            if (!pl.videoIds.contains(videoId)) {
                val updatedIds = pl.videoIds.toMutableList().apply { add(videoId) }
                current[index] = pl.copy(videoIds = updatedIds)
                persistPlaylists(current)
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, videoId: String) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val pl = current[index]
            val updatedTracks = pl.tracks.filter { it.id != videoId }
            val updatedIds = updatedTracks.map { it.id }
            val cover = if (updatedTracks.isEmpty()) null else pl.coverUrl
            current[index] = pl.copy(videoIds = updatedIds, tracks = updatedTracks, coverUrl = cover)
            persistPlaylists(current)
        }
    }

    private fun persistPlaylists(playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { pl ->
            val obj = JSONObject().apply {
                put("id", pl.id)
                put("title", pl.title)
                put("description", pl.description)
                put("coverUrl", pl.coverUrl ?: "")
                put("createdAt", pl.createdAt)
                val idArr = JSONArray()
                pl.videoIds.forEach { idArr.put(it) }
                put("videoIds", idArr)

                val tracksArr = JSONArray()
                pl.tracks.forEach { trk ->
                    tracksArr.put(videoToJson(trk))
                }
                put("tracks", tracksArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    // Activity & History Logging
    fun getActivityEvents(): List<ActivityEvent> {
        val jsonStr = prefs.getString(KEY_ACTIVITY_LOG, null) ?: return emptyList()
        val list = mutableListOf<ActivityEvent>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val typeStr = obj.optString("type", ActivityType.PLAYED_SONG.name)
                val type = try { ActivityType.valueOf(typeStr) } catch (_: Exception) { ActivityType.PLAYED_SONG }
                list.add(
                    ActivityEvent(
                        id = obj.getString("id"),
                        type = type,
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        trackId = obj.optString("trackId").ifBlank { null },
                        playlistId = obj.optString("playlistId").ifBlank { null }
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun logActivityEvent(event: ActivityEvent) {
        val current = getActivityEvents().toMutableList()
        current.add(0, event)
        val limited = current.take(MAX_ACTIVITIES)
        val arr = JSONArray()
        limited.forEach { ev ->
            val obj = JSONObject().apply {
                put("id", ev.id)
                put("type", ev.type.name)
                put("title", ev.title)
                put("description", ev.description)
                put("timestamp", ev.timestamp)
                put("trackId", ev.trackId ?: "")
                put("playlistId", ev.playlistId ?: "")
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_ACTIVITY_LOG, arr.toString()).apply()
    }

    fun clearActivityEvents() {
        prefs.edit().remove(KEY_ACTIVITY_LOG).apply()
    }

    // Settings
    fun getRepeatMode(): RepeatMode {
        val name = prefs.getString(KEY_REPEAT_MODE, RepeatMode.ALL.name) ?: RepeatMode.ALL.name
        return try { RepeatMode.valueOf(name) } catch (_: Exception) { RepeatMode.ALL }
    }

    fun setRepeatMode(mode: RepeatMode) {
        prefs.edit().putString(KEY_REPEAT_MODE, mode.name).apply()
    }

    fun isShuffleEnabled(): Boolean = prefs.getBoolean(KEY_SHUFFLE, false)

    fun setShuffleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE, enabled).apply()
    }

    fun getEqualizerPreset(): EqualizerPreset {
        val name = prefs.getString(KEY_EQUALIZER, EqualizerPreset.NORMAL.name) ?: EqualizerPreset.NORMAL.name
        return try { EqualizerPreset.valueOf(name) } catch (_: Exception) { EqualizerPreset.NORMAL }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        prefs.edit().putString(KEY_EQUALIZER, preset.name).apply()
    }

    fun getEqBand(band: String, defaultVal: Float): Float {
        return prefs.getFloat("eq_band_$band", defaultVal)
    }

    fun setEqBand(band: String, value: Float) {
        prefs.edit().putFloat("eq_band_$band", value).apply()
    }

    fun isBassBoost(): Boolean = prefs.getBoolean("eq_bass_boost", false)

    fun setBassBoost(enabled: Boolean) {
        prefs.edit().putBoolean("eq_bass_boost", enabled).apply()
    }

    fun isVirtualizer(): Boolean = prefs.getBoolean("eq_virtualizer", false)

    fun setVirtualizer(enabled: Boolean) {
        prefs.edit().putBoolean("eq_virtualizer", enabled).apply()
    }

    // Helper JSON conversions
    private fun parseSingleVideoJson(obj: JSONObject): YouTubeVideo {
        val id = obj.getString("id")
        val rawThumb = obj.optString("thumbnailUrl")
        val thumb = if (rawThumb.isNotBlank()) rawThumb else "https://i.ytimg.com/vi/$id/hqdefault.jpg"
        return YouTubeVideo(
            id = id,
            title = obj.optString("title", "Unknown Track"),
            channelTitle = obj.optString("channelTitle", "Unknown Artist"),
            channelThumbnailUrl = obj.optString("channelThumbnailUrl").ifBlank { null },
            thumbnailUrl = thumb,
            duration = obj.optString("duration").ifBlank { null },
            viewCountText = obj.optString("viewCountText").ifBlank { null },
            publishedTimeText = obj.optString("publishedTimeText").ifBlank { null },
            isLive = obj.optBoolean("isLive", false),
            description = obj.optString("description").ifBlank { null }
        )
    }

    private fun videoToJson(v: YouTubeVideo): JSONObject {
        return JSONObject().apply {
            put("id", v.id)
            put("title", v.title)
            put("channelTitle", v.channelTitle)
            put("channelThumbnailUrl", v.channelThumbnailUrl ?: "")
            put("thumbnailUrl", v.thumbnailUrl)
            put("duration", v.duration ?: "")
            put("viewCountText", v.viewCountText ?: "")
            put("publishedTimeText", v.publishedTimeText ?: "")
            put("isLive", v.isLive)
            put("description", v.description ?: "")
        }
    }

    private fun parseVideosJson(jsonStr: String): List<YouTubeVideo> {
        val list = mutableListOf<YouTubeVideo>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(parseSingleVideoJson(obj))
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveVideosJson(key: String, videos: List<YouTubeVideo>) {
        val arr = JSONArray()
        videos.forEach { v ->
            arr.put(videoToJson(v))
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    // Downloaded Tracks Methods
    fun getDownloadedTracks(): List<DownloadedTrack> {
        val jsonStr = prefs.getString(KEY_DOWNLOADED_TRACKS, null) ?: return emptyList()
        val list = mutableListOf<DownloadedTrack>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val videoObj = obj.getJSONObject("video")
                val video = parseSingleVideoJson(videoObj)
                val localFilePath = obj.optString("localFilePath")
                val fileSize = obj.optLong("fileSize", 0L)
                val downloadedAt = obj.optLong("downloadedAt", System.currentTimeMillis())
                val quality = obj.optString("audioQuality", "High (320 kbps)")
                list.add(
                    DownloadedTrack(
                        video = video,
                        localFilePath = localFilePath,
                        fileSize = fileSize,
                        downloadedAt = downloadedAt,
                        audioQuality = quality
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveDownloadedTrack(track: DownloadedTrack) {
        val current = getDownloadedTracks().filter { it.video.id != track.video.id }.toMutableList()
        current.add(0, track)
        saveDownloadedTracksList(current)
    }

    fun removeDownloadedTrack(videoId: String) {
        val current = getDownloadedTracks().filter { it.video.id != videoId }
        saveDownloadedTracksList(current)
    }

    fun isDownloaded(videoId: String): Boolean {
        return getDownloadedTracks().any { it.video.id == videoId }
    }

    fun clearAllDownloadedTracks() {
        prefs.edit().remove(KEY_DOWNLOADED_TRACKS).apply()
    }

    fun getDownloadQuality(): String {
        return prefs.getString(KEY_DOWNLOAD_QUALITY, "High (320 kbps)") ?: "High (320 kbps)"
    }

    fun setDownloadQuality(quality: String) {
        prefs.edit().putString(KEY_DOWNLOAD_QUALITY, quality).apply()
    }

    fun getStreamingQuality(): String {
        return prefs.getString(KEY_STREAMING_QUALITY, "Auto") ?: "Auto"
    }

    fun setStreamingQuality(quality: String) {
        prefs.edit().putString(KEY_STREAMING_QUALITY, quality).apply()
    }

    data class BackupImportResult(
        val playlistsCount: Int = 0,
        val likedCount: Int = 0,
        val recentsCount: Int = 0,
        val settingsRestored: Boolean = false,
        val errorMessage: String? = null
    )

    /**
     * Exports music preferences, custom playlists, liked songs, and settings into
     * standard human-readable JSON format for sharing and backup.
     */
    fun exportBackupJson(
        includePlaylists: Boolean = true,
        includeLikedSongs: Boolean = true,
        includeSettings: Boolean = true,
        includeRecents: Boolean = true
    ): String {
        val root = JSONObject()
        root.put("app", "VibeMusic")
        root.put("version", "2.4.0")
        root.put("exportedAt", System.currentTimeMillis())

        if (includePlaylists) {
            val plArr = JSONArray()
            getPlaylists().forEach { pl ->
                val obj = JSONObject().apply {
                    put("id", pl.id)
                    put("title", pl.title)
                    put("description", pl.description)
                    put("coverUrl", pl.coverUrl ?: "")
                    put("createdAt", pl.createdAt)
                    val idArr = JSONArray()
                    pl.videoIds.forEach { idArr.put(it) }
                    put("videoIds", idArr)

                    val tracksArr = JSONArray()
                    pl.tracks.forEach { trk -> tracksArr.put(videoToJson(trk)) }
                    put("tracks", tracksArr)
                }
                plArr.put(obj)
            }
            root.put("playlists", plArr)
        }

        if (includeLikedSongs) {
            val likedArr = JSONArray()
            getSavedVideos().forEach { likedArr.put(videoToJson(it)) }
            root.put("likedSongs", likedArr)
        }

        if (includeRecents) {
            val recentsArr = JSONArray()
            getRecentlyPlayed().forEach { recentsArr.put(videoToJson(it)) }
            root.put("recentlyPlayed", recentsArr)
        }

        if (includeSettings) {
            val settingsObj = JSONObject().apply {
                put("selectedTheme", getSelectedTheme())
                put("streamingQuality", getStreamingQuality())
                put("downloadQuality", getDownloadQuality())
                put("userName", getUserName())
                put("userEmail", getUserEmail())
                put("repeatMode", getRepeatMode().name)
                put("shuffle", isShuffleEnabled())
                put("equalizerPreset", getEqualizerPreset().name)
                put("bassBoost", isBassBoost())
                put("virtualizer", isVirtualizer())
            }
            root.put("settings", settingsObj)
        }

        return root.toString(2)
    }

    /**
     * Imports playlists, liked songs, and settings from a JSON backup string.
     */
    fun importBackupJson(jsonString: String, merge: Boolean = true): BackupImportResult {
        try {
            val root = JSONObject(jsonString)
            var restoredPlaylists = 0
            var restoredLiked = 0
            var restoredRecents = 0
            var restoredSettings = false

            // 1. Playlists
            if (root.has("playlists")) {
                val plArr = root.getJSONArray("playlists")
                val importedPlaylists = mutableListOf<Playlist>()
                for (i in 0 until plArr.length()) {
                    val obj = plArr.getJSONObject(i)
                    val id = obj.optString("id", "pl_${System.currentTimeMillis()}_$i")
                    val title = obj.optString("title", "Imported Playlist")
                    val desc = obj.optString("description", "")
                    val cover = obj.optString("coverUrl").ifBlank { null }
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                    val tracks = mutableListOf<YouTubeVideo>()
                    if (obj.has("tracks")) {
                        val tArr = obj.getJSONArray("tracks")
                        for (j in 0 until tArr.length()) {
                            tracks.add(parseSingleVideoJson(tArr.getJSONObject(j)))
                        }
                    }

                    val vIds = mutableListOf<String>()
                    if (obj.has("videoIds")) {
                        val idArr = obj.getJSONArray("videoIds")
                        for (k in 0 until idArr.length()) {
                            vIds.add(idArr.getString(k))
                        }
                    } else {
                        tracks.forEach { vIds.add(it.id) }
                    }

                    importedPlaylists.add(
                        Playlist(
                            id = id,
                            title = title,
                            description = desc,
                            coverUrl = cover,
                            createdAt = createdAt,
                            videoIds = vIds,
                            tracks = tracks
                        )
                    )
                }

                if (merge) {
                    val currentMap = getPlaylists().associateBy { it.id }.toMutableMap()
                    importedPlaylists.forEach { currentMap[it.id] = it }
                    persistPlaylists(currentMap.values.toList())
                } else {
                    persistPlaylists(importedPlaylists)
                }
                restoredPlaylists = importedPlaylists.size
            }

            // 2. Liked Songs
            if (root.has("likedSongs")) {
                val likedArr = root.getJSONArray("likedSongs")
                val importedLiked = mutableListOf<YouTubeVideo>()
                for (i in 0 until likedArr.length()) {
                    importedLiked.add(parseSingleVideoJson(likedArr.getJSONObject(i)))
                }

                if (merge) {
                    val currentMap = getSavedVideos().associateBy { it.id }.toMutableMap()
                    importedLiked.forEach { currentMap[it.id] = it }
                    saveSavedVideos(currentMap.values.toList())
                } else {
                    saveSavedVideos(importedLiked)
                }
                restoredLiked = importedLiked.size
            }

            // 3. Recently Played
            if (root.has("recentlyPlayed")) {
                val recentsArr = root.getJSONArray("recentlyPlayed")
                val importedRecents = mutableListOf<YouTubeVideo>()
                for (i in 0 until recentsArr.length()) {
                    importedRecents.add(parseSingleVideoJson(recentsArr.getJSONObject(i)))
                }
                val current = getRecentlyPlayed().toMutableList()
                importedRecents.forEach { r ->
                    if (current.none { it.id == r.id }) current.add(r)
                }
                saveRecentlyPlayed(current.take(MAX_RECENTS))
                restoredRecents = importedRecents.size
            }

            // 4. Settings
            if (root.has("settings")) {
                val s = root.getJSONObject("settings")
                if (s.has("selectedTheme")) setSelectedTheme(s.getString("selectedTheme"))
                if (s.has("streamingQuality")) setStreamingQuality(s.getString("streamingQuality"))
                if (s.has("downloadQuality")) setDownloadQuality(s.getString("downloadQuality"))
                if (s.has("userName")) setUserName(s.getString("userName"))
                if (s.has("userEmail")) setUserEmail(s.getString("userEmail"))
                if (s.has("repeatMode")) {
                    try {
                        setRepeatMode(RepeatMode.valueOf(s.getString("repeatMode")))
                    } catch (_: Exception) {}
                }
                if (s.has("shuffle")) setShuffleEnabled(s.getBoolean("shuffle"))
                if (s.has("equalizerPreset")) {
                    try {
                        setEqualizerPreset(EqualizerPreset.valueOf(s.getString("equalizerPreset")))
                    } catch (_: Exception) {}
                }
                if (s.has("bassBoost")) setBassBoost(s.getBoolean("bassBoost"))
                if (s.has("virtualizer")) setVirtualizer(s.getBoolean("virtualizer"))
                restoredSettings = true
            }

            return BackupImportResult(
                playlistsCount = restoredPlaylists,
                likedCount = restoredLiked,
                recentsCount = restoredRecents,
                settingsRestored = restoredSettings
            )
        } catch (e: Exception) {
            return BackupImportResult(errorMessage = e.message ?: "Invalid JSON format")
        }
    }

    fun saveDownloadedTracksList(list: List<DownloadedTrack>) {
        val arr = JSONArray()
        list.forEach { dt ->
            val obj = JSONObject().apply {
                put("video", videoToJson(dt.video))
                put("localFilePath", dt.localFilePath)
                put("fileSize", dt.fileSize)
                put("downloadedAt", dt.downloadedAt)
                put("audioQuality", dt.audioQuality)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_DOWNLOADED_TRACKS, arr.toString()).apply()
    }
}

