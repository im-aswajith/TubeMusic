package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalPreferencesRepository
import com.example.data.local.MusicDownloadManager
import com.example.data.model.ActivityEvent
import com.example.data.model.ActivityType
import com.example.data.model.DownloadedTrack
import com.example.data.model.EqualizerPreset
import com.example.data.model.PlayerDisplayMode
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.SearchFilterType
import com.example.data.model.YouTubeVideo
import com.example.data.network.YouTubeSearchRepository
import com.example.service.MusicPlaybackService
import com.example.ui.components.GlobalMusicPlayerHolder
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistProfile(
    val name: String,
    val imageUrl: String,
    val followersText: String = "2.4M monthly listeners",
    val isFollowing: Boolean = false,
    val bio: String = "Popular artist trending on charts and listening history.",
    val songs: List<YouTubeVideo> = emptyList()
)

data class RealArtistSection(
    val artistName: String,
    val imageUrl: String,
    val songs: List<YouTubeVideo>
)

data class AlbumDetail(
    val title: String,
    val artist: String,
    val coverUrl: String,
    val year: String = "2024",
    val tracksCount: Int = 10,
    val tracks: List<YouTubeVideo> = emptyList()
)

data class MusicPlayerUiState(
    // User Profile
    val userName: String = "Music Listener",
    val userEmail: String = "",
    val shouldQuitApp: Boolean = false,

    // Browsing & Search
    val query: String = "",
    val activeFilter: SearchFilterType = SearchFilterType.ALL,
    val activeSearchCategory: String = "Songs", // Songs, Artists, Albums, Playlists
    val isLoading: Boolean = false,
    val isSuggestionsLoading: Boolean = false,
    val liveSuggestions: List<String> = emptyList(),
    val searchResults: List<YouTubeVideo> = emptyList(),
    val quickPicks: List<YouTubeVideo> = emptyList(),
    val trendingTracks: List<YouTubeVideo> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val savedVideos: List<YouTubeVideo> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,
    val recentlyPlayed: List<YouTubeVideo> = emptyList(),
    val activityEvents: List<ActivityEvent> = emptyList(),
    val selectedCategory: String = "Trending Music",
    val errorMessage: String? = null,
    val currentTab: Int = 0, // 0: Home, 1: Search, 2: Library, 3: Profile

    // Music Playback State
    val currentTrack: YouTubeVideo? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<YouTubeVideo> = emptyList(),
    val queueIndex: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val isShuffleEnabled: Boolean = false,
    val playerDisplayMode: PlayerDisplayMode = PlayerDisplayMode.AUDIO_ARTWORK,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerMinutesRemaining: Int? = null,
    val sleepTimerSecondsRemaining: Int? = null,
    val equalizerPreset: EqualizerPreset = EqualizerPreset.NORMAL,

    // Audio & Settings State (from designv2 screens)
    val selectedTheme: String = "Dark", // Light, Dark, System
    val streamingQuality: String = "Auto",
    val downloadQuality: String = "High (320 kbps)",
    val crossfadeSeconds: Int = 3,
    val isGaplessPlayback: Boolean = true,
    val isAutoplay: Boolean = true,
    val isNormalizeVolume: Boolean = true,
    val isPlayInOrder: Boolean = false,
    val band60Hz: Float = 0.5f,
    val band230Hz: Float = 0.5f,
    val band910Hz: Float = 0.5f,
    val band3600Hz: Float = 0.5f,
    val band14000Hz: Float = 0.5f,
    val isBassBoost: Boolean = false,
    val isVirtualizer: Boolean = false,

    // Modals, Sheets & Sub-screens
    val isNowPlayingSheetOpen: Boolean = false,
    val isLyricsSheetOpen: Boolean = false,
    val isQueueSheetOpen: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false,
    val trackForPlaylistSelection: YouTubeVideo? = null,
    val showSleepTimerDialog: Boolean = false,
    val showEqualizerDialog: Boolean = false,
    val showSpeedDialog: Boolean = false,
    val showPlaybackSettingsDialog: Boolean = false,
    val showAudioQualityDialog: Boolean = false,
    val showNotificationsDialog: Boolean = false,
    val showAppearanceDialog: Boolean = false,
    val selectedArtistDetail: ArtistProfile? = null,
    val selectedAlbumDetail: AlbumDetail? = null,
    val isLikedSongsScreenOpen: Boolean = false,

    // Spotify UX & Downloads State
    val downloadedVideos: List<DownloadedTrack> = emptyList(),
    val downloadingIds: Set<String> = emptySet(),
    val downloadProgressMap: Map<String, Float> = emptyMap(),
    val downloadStates: Map<String, com.example.data.model.DownloadStatus> = emptyMap(),
    val showStorageCacheDialog: Boolean = false,
    val storageFreedMessage: String? = null,
    val homeFilterCategory: String = "All", // "All", "Music", "Podcasts", "Downloaded"

    // Related Songs Matching Taste Automatically from YouTube
    val relatedTasteSongs: List<YouTubeVideo> = emptyList(),
    val isFetchingRelatedSongs: Boolean = false,

    // Backup & Restore (JSON)
    val showBackupRestoreDialog: Boolean = false,
    val backupRestoreMessage: String? = null,

    // Listening Statistics V2 & Offline Mode
    val totalListeningSeconds: Long = 0L,
    val mostPlayedStats: List<com.example.data.local.db.TopTrackStat> = emptyList(),
    val topArtistsStats: List<com.example.data.local.db.TopArtistStat> = emptyList(),
    val isOfflineModeOnly: Boolean = false,
    val showListeningStatsDialog: Boolean = false,
    val gesturesEnabled: Boolean = true
)

class VideoSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val searchRepo = YouTubeSearchRepository()
    private val localRepo = LocalPreferencesRepository(application.applicationContext)
    private val roomRepo = com.example.data.repository.RoomMusicRepository(application.applicationContext)
    private val downloadManager = MusicDownloadManager(application.applicationContext, localRepo, roomRepo)

    private val _uiState = MutableStateFlow(
        MusicPlayerUiState(
            userName = localRepo.getUserName(),
            userEmail = localRepo.getUserEmail(),
            selectedTheme = localRepo.getSelectedTheme(),
            streamingQuality = localRepo.getStreamingQuality(),
            downloadQuality = localRepo.getDownloadQuality(),
            downloadedVideos = downloadManager.getAllDownloadedTracks(),
            searchHistory = localRepo.getSearchHistory(),
            savedVideos = localRepo.getSavedVideos(),
            playlists = localRepo.getPlaylists(),
            recentlyPlayed = localRepo.getRecentlyPlayed(),
            activityEvents = localRepo.getActivityEvents(),
            repeatMode = localRepo.getRepeatMode(),
            isShuffleEnabled = localRepo.isShuffleEnabled(),
            equalizerPreset = localRepo.getEqualizerPreset(),
            band60Hz = localRepo.getEqBand("60", 0.5f),
            band230Hz = localRepo.getEqBand("230", 0.5f),
            band910Hz = localRepo.getEqBand("910", 0.5f),
            band3600Hz = localRepo.getEqBand("3600", 0.5f),
            band14000Hz = localRepo.getEqBand("14000", 0.5f),
            isBassBoost = localRepo.isBassBoost(),
            isVirtualizer = localRepo.isVirtualizer()
        )
    )
    val uiState: StateFlow<MusicPlayerUiState> = _uiState.asStateFlow()

    private var suggestionJob: Job? = null
    private var searchJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var progressTickerJob: Job? = null

    val trendingSearches = listOf(
        "ocean eyes",
        "billie eilish",
        "the weeknd",
        "chill vibes",
        "tame impala",
        "lofi beats",
        "arctic monkeys",
        "indie rock"
    )

    val quickMixes = listOf(
        Pair("Chill Mix", "#waves"),
        Pair("Focus Mix", "#study"),
        Pair("Workout Mix", "#energy"),
        Pair("Late Night", "#jazz"),
        Pair("Indie Vibe", "#acoustic"),
        Pair("Lo-Fi Beats", "#peaceful")
    )

    init {
        // Wire service callbacks for lockscreen/notification background controls
        MusicPlaybackService.onTogglePlayCallback = { togglePlayPause() }
        MusicPlaybackService.onNextCallback = { playNextTrack() }
        MusicPlaybackService.onPrevCallback = { playPreviousTrack() }
        MusicPlaybackService.onStopCallback = { pausePlayback() }
        MusicPlaybackService.onSeekCallback = { posMs -> seekTo(posMs) }

        // Observe download progress and active downloads
        viewModelScope.launch {
            downloadManager.activeDownloads.collect { downloading ->
                _uiState.update { it.copy(downloadingIds = downloading) }
            }
        }
        viewModelScope.launch {
            downloadManager.downloadProgress.collect { progressMap ->
                _uiState.update { it.copy(downloadProgressMap = progressMap) }
            }
        }
        viewModelScope.launch {
            downloadManager.downloadStates.collect { states ->
                _uiState.update { it.copy(downloadStates = states) }
            }
        }

        // Room database migration and reactive flows
        viewModelScope.launch {
            com.example.data.local.db.DatabaseMigrationHelper.migrateLegacyPreferencesIfNeeded(
                context = application.applicationContext,
                database = com.example.data.local.db.AppDatabase.getInstance(application.applicationContext),
                legacyRepo = localRepo
            )
        }

        viewModelScope.launch {
            roomRepo.likedTracksFlow.collect { likedList ->
                _uiState.update { it.copy(savedVideos = likedList) }
            }
        }

        viewModelScope.launch {
            roomRepo.playlistsFlow.collect { plList ->
                _uiState.update { it.copy(playlists = plList) }
            }
        }

        viewModelScope.launch {
            roomRepo.recentlyPlayedFlow.collect { recents ->
                _uiState.update { it.copy(recentlyPlayed = recents) }
            }
        }

        viewModelScope.launch {
            roomRepo.downloadsFlow.collect { dls ->
                _uiState.update { it.copy(downloadedVideos = dls) }
            }
        }

        viewModelScope.launch {
            roomRepo.totalListeningSecondsFlow.collect { totalSecs ->
                _uiState.update { it.copy(totalListeningSeconds = totalSecs) }
            }
        }

        viewModelScope.launch {
            roomRepo.mostPlayedTracksFlow.collect { topTracks ->
                _uiState.update { it.copy(mostPlayedStats = topTracks) }
            }
        }

        viewModelScope.launch {
            roomRepo.topArtistsFlow.collect { topArtists ->
                _uiState.update { it.copy(topArtistsStats = topArtists) }
            }
        }

        // Initial load with default trending music tracks
        loadMusicCategory("Trending Music")
        loadQuickPicks()
    }

    private fun loadQuickPicks() {
        viewModelScope.launch {
            try {
                val picks = searchRepo.searchVideos("popular hits songs 2026", SearchFilterType.ALL)
                val finalPicks = if (picks.isNotEmpty()) picks else searchRepo.getCuratedFallbackVideos("music")
                _uiState.update { it.copy(quickPicks = finalPicks) }
            } catch (_: Exception) {
                _uiState.update { it.copy(quickPicks = searchRepo.getCuratedFallbackVideos("music")) }
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, errorMessage = null) }

        suggestionJob?.cancel()
        if (newQuery.trim().isEmpty()) {
            _uiState.update { it.copy(liveSuggestions = emptyList(), isSuggestionsLoading = false) }
            return
        }

        suggestionJob = viewModelScope.launch {
            delay(200) // Debounce typing
            _uiState.update { it.copy(isSuggestionsLoading = true) }
            val suggestions = searchRepo.getLiveSuggestions(newQuery)
            _uiState.update {
                it.copy(
                    liveSuggestions = suggestions,
                    isSuggestionsLoading = false
                )
            }
        }
    }

    /**
     * Executed when the user submits a search query (e.g. clicks Keyboard Search IME button)
     */
    fun onSearchSubmitted(query: String = _uiState.value.query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        _uiState.update {
            it.copy(
                query = trimmed,
                liveSuggestions = emptyList(),
                currentTab = 1, // Ensure on Search tab
                isLoading = true,
                errorMessage = null
            )
        }

        localRepo.addSearchQuery(trimmed)
        refreshHistory()

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val results = searchRepo.searchVideos(trimmed, _uiState.value.activeFilter)
                val finalResults = if (results.isNotEmpty()) results else searchRepo.getCuratedFallbackVideos(trimmed)
                _uiState.update {
                    it.copy(
                        searchResults = finalResults,
                        isLoading = false,
                        errorMessage = if (finalResults.isEmpty()) "No songs found for \"$trimmed\"" else null
                    )
                }
            } catch (e: Exception) {
                val fallback = searchRepo.getCuratedFallbackVideos(trimmed)
                _uiState.update {
                    it.copy(
                        searchResults = fallback,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun onSearchCategoryChanged(category: String) {
        _uiState.update { it.copy(activeSearchCategory = category) }
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            onSearchSubmitted(currentQuery)
        }
    }

    fun onFilterChanged(filter: SearchFilterType) {
        if (_uiState.value.activeFilter == filter) return
        _uiState.update { it.copy(activeFilter = filter) }
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            onSearchSubmitted(currentQuery)
        } else {
            loadMusicCategory(_uiState.value.selectedCategory)
        }
    }

    fun onCategorySelected(category: String) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                activeFilter = if (category.contains("Live", ignoreCase = true)) SearchFilterType.LIVE else SearchFilterType.ALL
            )
        }
        loadMusicCategory(category)
    }

    private fun loadMusicCategory(category: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val searchQuery = when (category) {
                "Trending Music" -> "top music hits 2026 official"
                "Chill Mix" -> "chill vibes acoustic lo-fi relax"
                "Focus Mix" -> "deep focus ambient study music"
                "Workout Mix" -> "workout gym high energy bass music"
                "Late Night" -> "late night jazz piano atmospheric"
                "Indie Vibe" -> "indie acoustic guitar alternative"
                "Lo-Fi Beats" -> "lofi hip hop chill beats"
                else -> "$category songs"
            }
            val filter = _uiState.value.activeFilter

            try {
                val results = searchRepo.searchVideos(searchQuery, filter)
                val finalResults = if (results.isNotEmpty()) results else searchRepo.getCuratedFallbackVideos(searchQuery)
                _uiState.update {
                    it.copy(
                        searchResults = finalResults,
                        trendingTracks = finalResults,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                val fallback = searchRepo.getCuratedFallbackVideos(searchQuery)
                _uiState.update {
                    it.copy(
                        searchResults = fallback,
                        trendingTracks = fallback,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSelectTab(tabIndex: Int) {
        _uiState.update {
            it.copy(
                currentTab = tabIndex,
                isLikedSongsScreenOpen = false,
                selectedArtistDetail = null,
                selectedAlbumDetail = null
            )
        }
        if (tabIndex == 2) {
            refreshSavedVideos()
            refreshPlaylists()
        }
    }

    // ==========================================
    // MUSIC PLAYBACK ENGINE CONTROLS
    // ==========================================

    fun playTrack(track: YouTubeVideo, contextQueue: List<YouTubeVideo> = emptyList()) {
        val newQueue = if (contextQueue.isNotEmpty()) {
            contextQueue
        } else if (_uiState.value.queue.isNotEmpty()) {
            _uiState.value.queue
        } else {
            listOf(track)
        }

        val idx = newQueue.indexOfFirst { it.id == track.id }.let { if (it >= 0) it else 0 }

        _uiState.update {
            it.copy(
                currentTrack = track,
                queue = newQueue,
                queueIndex = idx,
                isPlaying = true,
                isBuffering = true,
                playbackPositionMs = 0L,
                durationMs = track.durationSeconds * 1000L
            )
        }

        // Save to recently played in both Room and SharedPreferences
        localRepo.addRecentlyPlayed(track)
        viewModelScope.launch { roomRepo.addRecentlyPlayed(track) }
        refreshRecentlyPlayed()

        // Log played activity
        logActivity(
            ActivityType.PLAYED_SONG,
            "Listened to ${track.title}",
            "By ${track.channelTitle}",
            trackId = track.id
        )

        // Start background media service
        MusicPlaybackService.startServiceForTrack(
            getApplication<Application>().applicationContext,
            track,
            isPlaying = true,
            durationMs = track.durationSeconds * 1000L
        )

        startProgressTicker()
        fetchRelatedSongsForTrack(track)
    }

    private var relatedSongsJob: Job? = null

    /**
     * Automatically fetches related songs fitting user's listening taste from YouTube.
     */
    fun fetchRelatedSongsForTrack(track: YouTubeVideo) {
        relatedSongsJob?.cancel()
        relatedSongsJob = viewModelScope.launch {
            _uiState.update { it.copy(isFetchingRelatedSongs = true) }
            try {
                val artist = track.displayArtist.ifBlank { track.channelTitle }
                val query = if (artist.isNotBlank() && !artist.equals("Various Artists", ignoreCase = true)) {
                    "$artist mix songs"
                } else {
                    "${track.title} related music"
                }
                val results = searchRepo.searchVideos(query)
                val existingIds = (_uiState.value.queue.map { it.id } + track.id).toSet()
                val filtered = results.filter { !existingIds.contains(it.id) && !it.isLive }
                val finalRelated = if (filtered.isNotEmpty()) filtered.take(15) else {
                    _uiState.value.trendingTracks.filter { !existingIds.contains(it.id) }.take(8)
                }
                _uiState.update { it.copy(relatedTasteSongs = finalRelated, isFetchingRelatedSongs = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isFetchingRelatedSongs = false) }
            }
        }
    }

    fun togglePlayPause() {
        val current = _uiState.value.currentTrack ?: return
        val newPlayState = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = newPlayState) }

        MusicPlaybackService.updatePlaybackStatus(
            getApplication<Application>().applicationContext,
            newPlayState
        )

        if (newPlayState) {
            startProgressTicker()
        } else {
            progressTickerJob?.cancel()
        }
    }

    fun pausePlayback() {
        _uiState.update { it.copy(isPlaying = false) }
        MusicPlaybackService.updatePlaybackStatus(
            getApplication<Application>().applicationContext,
            false
        )
        progressTickerJob?.cancel()
    }

    fun resumePlayback() {
        if (_uiState.value.currentTrack != null) {
            _uiState.update { it.copy(isPlaying = true) }
            MusicPlaybackService.updatePlaybackStatus(
                getApplication<Application>().applicationContext,
                true
            )
            startProgressTicker()
        }
    }

    fun playNextTrack() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return

        val nextIndex = if (state.isShuffleEnabled) {
            (state.queue.indices).filter { it != state.queueIndex }.randomOrNull() ?: 0
        } else {
            (state.queueIndex + 1) % state.queue.size
        }

        if (nextIndex < state.queue.size && (state.repeatMode == RepeatMode.ALL || nextIndex > state.queueIndex)) {
            val nextTrack = state.queue[nextIndex]
            _uiState.update {
                it.copy(
                    currentTrack = nextTrack,
                    queueIndex = nextIndex,
                    isPlaying = true,
                    isBuffering = true,
                    playbackPositionMs = 0L,
                    durationMs = nextTrack.durationSeconds * 1000L
                )
            }
            localRepo.addRecentlyPlayed(nextTrack)
            refreshRecentlyPlayed()

            MusicPlaybackService.startServiceForTrack(
                getApplication<Application>().applicationContext,
                nextTrack,
                isPlaying = true
            )
            fetchRelatedSongsForTrack(nextTrack)
        } else if (state.isAutoplay && state.relatedTasteSongs.isNotEmpty()) {
            val autoTrack = state.relatedTasteSongs.first()
            val remaining = state.relatedTasteSongs.drop(1)
            val updatedQueue = state.queue + autoTrack
            val newIdx = updatedQueue.size - 1

            _uiState.update {
                it.copy(
                    relatedTasteSongs = remaining,
                    queue = updatedQueue,
                    queueIndex = newIdx,
                    currentTrack = autoTrack,
                    isPlaying = true,
                    isBuffering = true,
                    playbackPositionMs = 0L,
                    durationMs = autoTrack.durationSeconds * 1000L
                )
            }
            localRepo.addRecentlyPlayed(autoTrack)
            refreshRecentlyPlayed()

            MusicPlaybackService.startServiceForTrack(
                getApplication<Application>().applicationContext,
                autoTrack,
                isPlaying = true
            )
            fetchRelatedSongsForTrack(autoTrack)
        }
    }

    fun playPreviousTrack() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return

        // If played more than 3 seconds, restart current track
        if (state.playbackPositionMs > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (state.queueIndex > 0) state.queueIndex - 1 else state.queue.size - 1
        if (prevIndex in state.queue.indices) {
            val prevTrack = state.queue[prevIndex]
            _uiState.update {
                it.copy(
                    currentTrack = prevTrack,
                    queueIndex = prevIndex,
                    isPlaying = true,
                    isBuffering = true,
                    playbackPositionMs = 0L,
                    durationMs = prevTrack.durationSeconds * 1000L
                )
            }
            localRepo.addRecentlyPlayed(prevTrack)
            refreshRecentlyPlayed()

            MusicPlaybackService.startServiceForTrack(
                getApplication<Application>().applicationContext,
                prevTrack,
                isPlaying = true
            )
        }
    }

    fun seekTo(positionMs: Long) {
        val dur = _uiState.value.durationMs
        val clamped = if (dur > 0) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
        _uiState.update { it.copy(playbackPositionMs = clamped) }
        GlobalMusicPlayerHolder.seekToDirectly(clamped / 1000f)
        MusicPlaybackService.updateProgress(
            getApplication<Application>().applicationContext,
            clamped,
            dur
        )
    }

    fun onTrackEnded() {
        val state = _uiState.value
        state.currentTrack?.let { current ->
            val playedSec = (state.playbackPositionMs / 1000L).coerceAtLeast(10L)
            viewModelScope.launch { roomRepo.recordPlay(current, playedSec) }
        }
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0L)
                _uiState.update { it.copy(isPlaying = true) }
            }
            RepeatMode.ALL -> {
                playNextTrack()
            }
            RepeatMode.OFF -> {
                if (state.queueIndex < state.queue.size - 1) {
                    playNextTrack()
                } else {
                    _uiState.update { it.copy(isPlaying = false) }
                    MusicPlaybackService.updatePlaybackStatus(
                        getApplication<Application>().applicationContext,
                        false
                    )
                }
            }
        }
    }

    fun updatePlaybackProgress(positionSec: Float, durationSec: Float) {
        val posMs = (positionSec * 1000).toLong()
        val durMs = if (durationSec > 0) (durationSec * 1000).toLong() else _uiState.value.durationMs
        _uiState.update {
            it.copy(
                playbackPositionMs = posMs,
                durationMs = if (durMs > 0) durMs else it.durationMs,
                isBuffering = false
            )
        }
        MusicPlaybackService.updateProgress(
            getApplication<Application>().applicationContext,
            posMs,
            if (durMs > 0) durMs else _uiState.value.durationMs
        )
    }

    fun setPlayerStateFromBridge(isPlaying: Boolean, isBuffering: Boolean) {
        _uiState.update {
            it.copy(isPlaying = isPlaying, isBuffering = isBuffering)
        }
        MusicPlaybackService.updatePlaybackStatus(
            getApplication<Application>().applicationContext,
            isPlaying
        )
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = viewModelScope.launch {
            while (_uiState.value.isPlaying) {
                delay(1000)
                val current = _uiState.value
                if (current.isPlaying && current.durationMs > 0) {
                    val nextPos = (current.playbackPositionMs + (1000 * current.playbackSpeed).toLong())
                    if (nextPos >= current.durationMs) {
                        onTrackEnded()
                    } else {
                        _uiState.update { it.copy(playbackPositionMs = nextPos) }
                        MusicPlaybackService.updateProgress(
                            getApplication<Application>().applicationContext,
                            nextPos,
                            current.durationMs
                        )
                    }
                }
            }
        }
    }

    // Controls & Settings
    fun toggleShuffle() {
        val newShuffle = !_uiState.value.isShuffleEnabled
        localRepo.setShuffleEnabled(newShuffle)
        _uiState.update { it.copy(isShuffleEnabled = newShuffle) }
    }

    fun toggleRepeatMode() {
        val current = _uiState.value.repeatMode
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        localRepo.setRepeatMode(next)
        _uiState.update { it.copy(repeatMode = next) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed, showSpeedDialog = false) }
    }

    fun setPlayerDisplayMode(mode: PlayerDisplayMode) {
        _uiState.update { it.copy(playerDisplayMode = mode) }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        localRepo.setEqualizerPreset(preset)
        val (b60, b230, b910, b3600, b14000) = when (preset) {
            EqualizerPreset.NORMAL -> listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
            EqualizerPreset.BASS_BOOST -> listOf(0.85f, 0.70f, 0.50f, 0.45f, 0.45f)
            EqualizerPreset.VOCAL_BOOST -> listOf(0.40f, 0.55f, 0.85f, 0.75f, 0.60f)
            EqualizerPreset.ACOUSTIC -> listOf(0.60f, 0.55f, 0.65f, 0.70f, 0.75f)
            EqualizerPreset.EDM_CLUB -> listOf(0.90f, 0.65f, 0.45f, 0.75f, 0.85f)
            EqualizerPreset.ROCK -> listOf(0.75f, 0.60f, 0.45f, 0.70f, 0.80f)
            EqualizerPreset.LOFI_CHILL -> listOf(0.70f, 0.60f, 0.50f, 0.40f, 0.35f)
        }
        localRepo.setEqBand("60", b60)
        localRepo.setEqBand("230", b230)
        localRepo.setEqBand("910", b910)
        localRepo.setEqBand("3600", b3600)
        localRepo.setEqBand("14000", b14000)

        _uiState.update {
            it.copy(
                equalizerPreset = preset,
                band60Hz = b60,
                band230Hz = b230,
                band910Hz = b910,
                band3600Hz = b3600,
                band14000Hz = b14000
            )
        }
    }

    fun setFrequencyBand(bandIndex: Int, value: Float) {
        val clamped = value.coerceIn(0.0f, 1.0f)
        when (bandIndex) {
            0 -> {
                localRepo.setEqBand("60", clamped)
                _uiState.update { it.copy(band60Hz = clamped) }
            }
            1 -> {
                localRepo.setEqBand("230", clamped)
                _uiState.update { it.copy(band230Hz = clamped) }
            }
            2 -> {
                localRepo.setEqBand("910", clamped)
                _uiState.update { it.copy(band910Hz = clamped) }
            }
            3 -> {
                localRepo.setEqBand("3600", clamped)
                _uiState.update { it.copy(band3600Hz = clamped) }
            }
            4 -> {
                localRepo.setEqBand("14000", clamped)
                _uiState.update { it.copy(band14000Hz = clamped) }
            }
        }
    }

    fun setSleepTimer(minutes: Int?, endOfSong: Boolean = false) {
        sleepTimerJob?.cancel()
        if ((minutes == null || minutes <= 0) && !endOfSong) {
            _uiState.update {
                it.copy(
                    sleepTimerMinutesRemaining = null,
                    sleepTimerSecondsRemaining = null,
                    showSleepTimerDialog = false
                )
            }
            GlobalMusicPlayerHolder.setVolume(1.0f)
            return
        }

        val totalSec = if (endOfSong) {
            val cur = _uiState.value.playbackPositionMs
            val dur = _uiState.value.durationMs
            if (dur > cur) ((dur - cur) / 1000).toInt().coerceAtLeast(5) else 180
        } else {
            (minutes ?: 15) * 60
        }

        _uiState.update {
            it.copy(
                sleepTimerMinutesRemaining = (totalSec + 59) / 60,
                sleepTimerSecondsRemaining = totalSec,
                showSleepTimerDialog = false
            )
        }

        sleepTimerJob = viewModelScope.launch {
            var rem = totalSec
            while (rem > 0) {
                delay(1000L)
                rem--
                // Gradual volume reduction during final 15 seconds
                if (rem in 1..15) {
                    val vol = (rem / 15f).coerceIn(0.1f, 1f)
                    GlobalMusicPlayerHolder.setVolume(vol)
                }
                _uiState.update {
                    it.copy(
                        sleepTimerSecondsRemaining = if (rem > 0) rem else null,
                        sleepTimerMinutesRemaining = if (rem > 0) (rem + 59) / 60 else null
                    )
                }
            }

            // Gracefully stop playback without terminating the process
            pausePlayback()
            GlobalMusicPlayerHolder.setVolume(1.0f)
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    sleepTimerMinutesRemaining = null,
                    sleepTimerSecondsRemaining = null
                )
            }
            MusicPlaybackService.updatePlaybackStatus(getApplication<Application>().applicationContext, false)
        }
    }

    fun toggleOfflineModeOnly() {
        _uiState.update { it.copy(isOfflineModeOnly = !it.isOfflineModeOnly) }
    }

    fun showListeningStatsDialog(show: Boolean) {
        _uiState.update { it.copy(showListeningStatsDialog = show) }
    }

    fun getSmartPlaylists(): List<Playlist> {
        val state = _uiState.value
        val liked = Playlist(
            id = "smart_liked",
            title = "Liked Songs",
            description = "Your favorite tracks in one place",
            coverUrl = state.savedVideos.firstOrNull()?.thumbnailUrl,
            videoIds = state.savedVideos.map { it.id },
            tracks = state.savedVideos
        )
        val recents = Playlist(
            id = "smart_recents",
            title = "Recently Played",
            description = "Tracks you listened to recently",
            coverUrl = state.recentlyPlayed.firstOrNull()?.thumbnailUrl,
            videoIds = state.recentlyPlayed.map { it.id },
            tracks = state.recentlyPlayed
        )
        val downloads = Playlist(
            id = "smart_downloads",
            title = "Downloaded Offline",
            description = "Available without internet connection",
            coverUrl = state.downloadedVideos.firstOrNull()?.video?.thumbnailUrl,
            videoIds = state.downloadedVideos.map { it.video.id },
            tracks = state.downloadedVideos.map { it.video }
        )
        return listOf(liked, recents, downloads)
    }

    fun acknowledgeQuitApp() {
        _uiState.update { it.copy(shouldQuitApp = false) }
    }

    fun setTheme(theme: String) {
        localRepo.setSelectedTheme(theme)
        _uiState.update { it.copy(selectedTheme = theme, showAppearanceDialog = false) }
    }

    fun setUserName(name: String) {
        localRepo.setUserName(name)
        _uiState.update { it.copy(userName = name) }
    }

    fun setStreamingQuality(quality: String) {
        localRepo.setStreamingQuality(quality)
        _uiState.update { it.copy(streamingQuality = quality) }
    }

    fun setDownloadQuality(quality: String) {
        localRepo.setDownloadQuality(quality)
        _uiState.update { it.copy(downloadQuality = quality) }
    }

    fun setCrossfade(seconds: Int) {
        _uiState.update { it.copy(crossfadeSeconds = seconds) }
    }

    fun toggleGapless() {
        _uiState.update { it.copy(isGaplessPlayback = !it.isGaplessPlayback) }
    }

    fun toggleAutoplay() {
        _uiState.update { it.copy(isAutoplay = !it.isAutoplay) }
    }

    fun toggleNormalizeVolume() {
        _uiState.update { it.copy(isNormalizeVolume = !it.isNormalizeVolume) }
    }

    fun togglePlayInOrder() {
        _uiState.update { it.copy(isPlayInOrder = !it.isPlayInOrder) }
    }

    fun toggleBassBoost() {
        val next = !_uiState.value.isBassBoost
        localRepo.setBassBoost(next)
        _uiState.update { it.copy(isBassBoost = next) }
    }

    fun toggleVirtualizer() {
        val next = !_uiState.value.isVirtualizer
        localRepo.setVirtualizer(next)
        _uiState.update { it.copy(isVirtualizer = next) }
    }

    // Queue Management
    fun addToQueue(track: YouTubeVideo) {
        val current = _uiState.value.queue.toMutableList()
        current.add(track)
        _uiState.update { it.copy(queue = current) }
    }

    fun playNextInQueue(track: YouTubeVideo) {
        val current = _uiState.value.queue.toMutableList()
        val insertIndex = (_uiState.value.queueIndex + 1).coerceAtMost(current.size)
        current.add(insertIndex, track)
        _uiState.update { it.copy(queue = current) }
    }

    fun removeFromQueue(index: Int) {
        val current = _uiState.value.queue.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val newIdx = if (index < _uiState.value.queueIndex) _uiState.value.queueIndex - 1 else _uiState.value.queueIndex
            _uiState.update { it.copy(queue = current, queueIndex = newIdx.coerceAtLeast(0)) }
        }
    }

    fun clearQueue() {
        val current = _uiState.value.currentTrack
        _uiState.update {
            it.copy(
                queue = if (current != null) listOf(current) else emptyList(),
                queueIndex = 0
            )
        }
    }

    fun addRelatedSongToQueue(track: YouTubeVideo) {
        val current = _uiState.value.queue.toMutableList()
        if (current.none { it.id == track.id }) {
            current.add(track)
            _uiState.update { it.copy(queue = current) }
        }
    }

    fun playRelatedSongNext(track: YouTubeVideo) {
        val current = _uiState.value.queue.toMutableList()
        current.removeAll { it.id == track.id }
        val insertIndex = (_uiState.value.queueIndex + 1).coerceAtMost(current.size)
        current.add(insertIndex, track)
        _uiState.update { it.copy(queue = current) }
    }

    fun addAllRelatedSongsToQueue() {
        val current = _uiState.value.queue.toMutableList()
        val toAdd = _uiState.value.relatedTasteSongs.filter { rel -> current.none { it.id == rel.id } }
        current.addAll(toAdd)
        _uiState.update { it.copy(queue = current) }
    }

    // Backup & Restore
    fun showBackupRestoreDialog(show: Boolean) {
        _uiState.update { it.copy(showBackupRestoreDialog = show, backupRestoreMessage = null) }
    }

    fun exportBackupJson(
        includePlaylists: Boolean = true,
        includeLiked: Boolean = true,
        includeSettings: Boolean = true,
        includeRecents: Boolean = true
    ): String {
        return localRepo.exportBackupJson(includePlaylists, includeLiked, includeSettings, includeRecents)
    }

    fun importBackupJson(jsonString: String, merge: Boolean = true): LocalPreferencesRepository.BackupImportResult {
        val result = localRepo.importBackupJson(jsonString, merge)
        if (result.errorMessage == null) {
            refreshPlaylists()
            refreshSavedVideos()
            refreshRecentlyPlayed()
            _uiState.update {
                it.copy(
                    selectedTheme = localRepo.getSelectedTheme(),
                    streamingQuality = localRepo.getStreamingQuality(),
                    downloadQuality = localRepo.getDownloadQuality(),
                    userName = localRepo.getUserName(),
                    userEmail = localRepo.getUserEmail(),
                    repeatMode = localRepo.getRepeatMode(),
                    isShuffleEnabled = localRepo.isShuffleEnabled(),
                    equalizerPreset = localRepo.getEqualizerPreset(),
                    isBassBoost = localRepo.isBassBoost(),
                    isVirtualizer = localRepo.isVirtualizer(),
                    backupRestoreMessage = "Successfully imported ${result.playlistsCount} playlists and ${result.likedCount} liked songs!"
                )
            }
        } else {
            _uiState.update { it.copy(backupRestoreMessage = "Import failed: ${result.errorMessage}") }
        }
        return result
    }

    fun shareBackupJson(context: Context, jsonString: String, title: String = "VibeMusic_Backup.json") {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "VibeMusic Playlist & Preferences Backup")
                putExtra(Intent.EXTRA_TEXT, jsonString)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Backup JSON"))
        } catch (_: Exception) {}
    }

    // Sheets & Dialog visibility triggers
    fun openNowPlayingSheet(open: Boolean) {
        _uiState.update { it.copy(isNowPlayingSheetOpen = open) }
    }

    fun openLyricsSheet(open: Boolean) {
        _uiState.update { it.copy(isLyricsSheetOpen = open) }
    }

    fun openQueueSheet(open: Boolean) {
        _uiState.update { it.copy(isQueueSheetOpen = open) }
    }

    fun openLikedSongsScreen(open: Boolean) {
        _uiState.update { it.copy(isLikedSongsScreenOpen = open) }
    }

    fun openArtistDetail(artist: ArtistProfile?) {
        if (artist != null) {
            val songs = if (artist.songs.isNotEmpty()) artist.songs else getSongsForArtist(artist.name)
            val updated = artist.copy(songs = songs)
            _uiState.update { it.copy(selectedArtistDetail = updated) }
        } else {
            _uiState.update { it.copy(selectedArtistDetail = null) }
        }
    }

    /**
     * Extracts real artists dynamically from recently played songs (and fallback curated tracks)
     * along with their authentic song lists.
     */
    fun getArtistsBasedOnRecentlyPlayed(): List<RealArtistSection> {
        val state = _uiState.value
        val sourceTracks = (state.recentlyPlayed + state.savedVideos + state.trendingTracks + state.quickPicks)
            .distinctBy { it.id }

        val artistSongsMap = linkedMapOf<String, MutableList<YouTubeVideo>>()
        val artistThumbMap = mutableMapOf<String, String>()

        for (track in sourceTracks) {
            val artist = track.displayArtist.trim()
            if (artist.isNotBlank() && !artist.equals("Unknown", ignoreCase = true) && !artist.equals("Artist", ignoreCase = true)) {
                val list = artistSongsMap.getOrPut(artist) { mutableListOf() }
                if (list.none { it.id == track.id }) {
                    list.add(track)
                }
                if (!artistThumbMap.containsKey(artist) && track.thumbnailUrl.isNotBlank()) {
                    artistThumbMap[artist] = track.thumbnailUrl
                }
            }
        }

        return artistSongsMap.entries.map { (name, songs) ->
            RealArtistSection(
                artistName = name,
                imageUrl = artistThumbMap[name] ?: songs.firstOrNull()?.thumbnailUrl ?: "",
                songs = songs
            )
        }.take(8)
    }

    fun getSongsForArtist(artistName: String): List<YouTubeVideo> {
        val state = _uiState.value
        val allTracks = (state.recentlyPlayed + state.savedVideos + state.trendingTracks + state.searchResults + state.quickPicks)
            .distinctBy { it.id }
        val matching = allTracks.filter {
            it.displayArtist.contains(artistName, ignoreCase = true) ||
            it.channelTitle.contains(artistName, ignoreCase = true) ||
            it.title.contains(artistName, ignoreCase = true)
        }
        return if (matching.isNotEmpty()) matching else state.trendingTracks.take(8)
    }

    fun openAlbumDetail(album: AlbumDetail?) {
        _uiState.update { it.copy(selectedAlbumDetail = album) }
    }

    fun showCreatePlaylistDialog(show: Boolean) {
        _uiState.update { it.copy(showCreatePlaylistDialog = show) }
    }

    fun showAddToPlaylistDialog(track: YouTubeVideo?) {
        _uiState.update { it.copy(trackForPlaylistSelection = track) }
    }

    fun showSleepTimerDialog(show: Boolean) {
        _uiState.update { it.copy(showSleepTimerDialog = show) }
    }

    fun showEqualizerDialog(show: Boolean) {
        _uiState.update { it.copy(showEqualizerDialog = show) }
    }

    fun showSpeedDialog(show: Boolean) {
        _uiState.update { it.copy(showSpeedDialog = show) }
    }

    fun showPlaybackSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showPlaybackSettingsDialog = show) }
    }

    fun showAudioQualityDialog(show: Boolean) {
        _uiState.update { it.copy(showAudioQualityDialog = show) }
    }

    fun showNotificationsDialog(show: Boolean) {
        _uiState.update { it.copy(showNotificationsDialog = show) }
    }

    fun showAppearanceDialog(show: Boolean) {
        _uiState.update { it.copy(showAppearanceDialog = show) }
    }

    // Playlist Management
    fun createPlaylist(title: String, description: String) {
        val id = "pl_" + System.currentTimeMillis()
        val cleanTitle = title.ifBlank { "My Playlist" }
        val newPl = Playlist(
            id = id,
            title = cleanTitle,
            description = description,
            coverUrl = null,
            videoIds = emptyList(),
            tracks = emptyList()
        )
        localRepo.savePlaylist(newPl)
        viewModelScope.launch {
            roomRepo.createPlaylist(cleanTitle, description)
        }
        refreshPlaylists()
        logActivity(
            ActivityType.CREATED_PLAYLIST,
            "Created playlist \"$cleanTitle\"",
            "New custom playlist created",
            playlistId = id
        )
        showCreatePlaylistDialog(false)
    }

    fun deletePlaylist(playlistId: String) {
        val pl = _uiState.value.playlists.find { it.id == playlistId }
        val plTitle = pl?.title ?: "Playlist"
        localRepo.deletePlaylist(playlistId)
        viewModelScope.launch {
            roomRepo.deletePlaylist(playlistId)
        }
        if (_uiState.value.selectedPlaylist?.id == playlistId) {
            _uiState.update { it.copy(selectedPlaylist = null) }
        }
        refreshPlaylists()
        logActivity(
            ActivityType.DELETED_PLAYLIST,
            "Deleted playlist \"$plTitle\"",
            "Removed playlist from your library",
            playlistId = playlistId
        )
    }

    fun addTrackToPlaylist(playlistId: String, video: YouTubeVideo) {
        localRepo.addTrackToPlaylist(playlistId, video)
        viewModelScope.launch {
            roomRepo.addTrackToPlaylist(playlistId, video)
        }
        val allPlaylists = localRepo.getPlaylists()
        val pl = allPlaylists.find { it.id == playlistId }
        val plTitle = pl?.title ?: "Playlist"
        _uiState.update { current ->
            current.copy(
                playlists = allPlaylists,
                selectedPlaylist = if (current.selectedPlaylist?.id == playlistId) pl else current.selectedPlaylist
            )
        }
        logActivity(
            ActivityType.ADDED_TO_PLAYLIST,
            "Added to $plTitle",
            "${video.title} added to playlist",
            trackId = video.id,
            playlistId = playlistId
        )
        showAddToPlaylistDialog(null)
    }

    fun removeTrackFromPlaylist(playlistId: String, videoId: String) {
        val allBefore = localRepo.getPlaylists()
        val plBefore = allBefore.find { it.id == playlistId }
        val track = plBefore?.tracks?.find { it.id == videoId }
        val trackTitle = track?.title ?: "Track"
        val plTitle = plBefore?.title ?: "Playlist"
        localRepo.removeTrackFromPlaylist(playlistId, videoId)
        viewModelScope.launch {
            roomRepo.removeTrackFromPlaylist(playlistId, videoId)
        }
        val allAfter = localRepo.getPlaylists()
        val plAfter = allAfter.find { it.id == playlistId }
        _uiState.update { current ->
            current.copy(
                playlists = allAfter,
                selectedPlaylist = if (current.selectedPlaylist?.id == playlistId) plAfter else current.selectedPlaylist
            )
        }
        logActivity(
            ActivityType.REMOVED_FROM_PLAYLIST,
            "Removed from $plTitle",
            "$trackTitle removed from playlist",
            trackId = videoId,
            playlistId = playlistId
        )
    }

    fun selectPlaylist(playlist: Playlist?) {
        val resolved = if (playlist != null) {
            localRepo.getPlaylists().find { it.id == playlist.id } ?: playlist
        } else null
        _uiState.update { it.copy(selectedPlaylist = resolved) }
    }

    // Liked & History
    fun toggleSaveVideo(video: YouTubeVideo) {
        viewModelScope.launch {
            val isNowSaved = roomRepo.toggleLikedTrack(video)
            localRepo.toggleSavedVideo(video)
            refreshSavedVideos()
            if (isNowSaved) {
                logActivity(
                    ActivityType.LIKED_SONG,
                    "Liked ${video.title}",
                    "Added to your Liked Songs library",
                    trackId = video.id
                )
            } else {
                logActivity(
                    ActivityType.UNLIKED_SONG,
                    "Unliked ${video.title}",
                    "Removed from Liked Songs",
                    trackId = video.id
                )
            }
        }
    }

    fun logActivity(
        type: ActivityType,
        title: String,
        description: String,
        trackId: String? = null,
        playlistId: String? = null
    ) {
        val event = ActivityEvent(
            id = UUID.randomUUID().toString(),
            type = type,
            title = title,
            description = description,
            timestamp = System.currentTimeMillis(),
            trackId = trackId,
            playlistId = playlistId
        )
        localRepo.logActivityEvent(event)
        _uiState.update { it.copy(activityEvents = localRepo.getActivityEvents()) }
    }

    fun clearActivityEvents() {
        localRepo.clearActivityEvents()
        _uiState.update { it.copy(activityEvents = emptyList()) }
    }

    fun isVideoSaved(videoId: String): Boolean {
        return localRepo.isVideoSaved(videoId)
    }

    fun removeHistoryItem(item: String) {
        localRepo.removeSearchQuery(item)
        refreshHistory()
    }

    fun clearAllHistory() {
        localRepo.clearSearchHistory()
        refreshHistory()
    }

    private fun refreshHistory() {
        _uiState.update { it.copy(searchHistory = localRepo.getSearchHistory()) }
    }

    private fun refreshSavedVideos() {
        _uiState.update { it.copy(savedVideos = localRepo.getSavedVideos()) }
    }

    private fun refreshPlaylists() {
        _uiState.update { it.copy(playlists = localRepo.getPlaylists()) }
    }

    private fun refreshRecentlyPlayed() {
        _uiState.update { it.copy(recentlyPlayed = localRepo.getRecentlyPlayed()) }
    }

    // Download & Offline Playback Methods
    fun isSongDownloaded(videoId: String): Boolean {
        return downloadManager.isDownloaded(videoId)
    }

    fun isSongDownloading(videoId: String): Boolean {
        return _uiState.value.downloadingIds.contains(videoId)
    }

    fun getDownloadProgress(videoId: String): Float {
        return _uiState.value.downloadProgressMap[videoId] ?: 0f
    }

    fun getDownloadedSongPath(videoId: String): String? {
        return downloadManager.getDownloadedTrack(videoId)?.localFilePath
    }

    fun downloadTrack(video: YouTubeVideo, quality: String = _uiState.value.downloadQuality) {
        viewModelScope.launch {
            val result = downloadManager.downloadSong(video, quality)
            val updated = downloadManager.getAllDownloadedTracks()
            _uiState.update {
                it.copy(
                    downloadedVideos = updated,
                    storageFreedMessage = if (result.isSuccess) "Downloaded \"${video.title}\" for offline playback" else "Failed to download track"
                )
            }
        }
    }

    fun cancelDownload(videoId: String) {
        downloadManager.cancelDownload(videoId)
        _uiState.update {
            it.copy(storageFreedMessage = "Download cancelled")
        }
    }

    fun retryDownload(video: YouTubeVideo, quality: String = _uiState.value.downloadQuality) {
        viewModelScope.launch {
            val result = downloadManager.retryDownload(video, quality)
            val updated = downloadManager.getAllDownloadedTracks()
            _uiState.update {
                it.copy(
                    downloadedVideos = updated,
                    storageFreedMessage = if (result.isSuccess) "Downloaded \"${video.title}\" for offline playback" else "Failed to download track"
                )
            }
        }
    }

    fun deleteDownloadedTrack(videoId: String) {
        viewModelScope.launch {
            val track = downloadManager.getDownloadedTrack(videoId)
            downloadManager.deleteDownloadedTrack(videoId)
            val updated = downloadManager.getAllDownloadedTracks()
            _uiState.update {
                it.copy(
                    downloadedVideos = updated,
                    storageFreedMessage = track?.let { t -> "Removed \"${t.video.title}\" from downloads" } ?: "Removed download"
                )
            }
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            val freed = downloadManager.clearAllDownloads()
            val updated = downloadManager.getAllDownloadedTracks()
            val freedStr = downloadManager.formatBytes(freed)
            _uiState.update {
                it.copy(
                    downloadedVideos = updated,
                    storageFreedMessage = "Deleted all downloaded songs (Freed $freedStr)"
                )
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val freed = downloadManager.clearAppCache()
            val freedStr = downloadManager.formatBytes(freed)
            _uiState.update {
                it.copy(storageFreedMessage = "Cleared temporary cache (Freed $freedStr)")
            }
        }
    }

    fun getDownloadedStorageSize(): String {
        return downloadManager.formatBytes(downloadManager.getDownloadedAudioSizeBytes())
    }

    fun getCacheStorageSize(): String {
        return downloadManager.formatBytes(downloadManager.getCacheSizeBytes())
    }

    fun setHomeFilterCategory(category: String) {
        _uiState.update { it.copy(homeFilterCategory = category) }
    }

    fun showStorageCacheDialog(show: Boolean) {
        _uiState.update { it.copy(showStorageCacheDialog = show) }
    }

    fun clearStorageFreedMessage() {
        _uiState.update { it.copy(storageFreedMessage = null) }
    }
}
