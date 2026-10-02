package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.model.PlayerDisplayMode
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AlbumDetailSheet
import com.example.ui.components.AppearanceDialog
import com.example.ui.components.ArtistProfileSheet
import com.example.ui.components.AudioQualityDialog
import com.example.ui.components.BackgroundMusicEngine
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.LyricsSheet
import com.example.ui.components.MiniMusicPlayer
import com.example.ui.components.MusicBottomBar
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.PlaybackSettingsDialog
import com.example.ui.components.PlaybackSpeedDialog
import com.example.ui.components.QueueBottomSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.StorageCacheDialog
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.VideoSearchViewModel

@Composable
fun HomeScreen(
    viewModel: VideoSearchViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Persistent Background Player Instance (Always rendered so playback continues seamlessly)
    BackgroundMusicEngine(
        currentTrack = uiState.currentTrack,
        isPlaying = uiState.isPlaying,
        playbackSpeed = uiState.playbackSpeed,
        seekToMs = -1L,
        displayMode = if (uiState.isNowPlayingSheetOpen) uiState.playerDisplayMode else PlayerDisplayMode.AUDIO_ARTWORK,
        isSheetOpen = uiState.isNowPlayingSheetOpen,
        viewModel = viewModel
    )

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("home_screen_scaffold"),
        containerColor = CanvasBg,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite)
            ) {
                // Docked Mini Music Player (Obsidian Black Pill)
                MiniMusicPlayer(
                    currentTrack = uiState.currentTrack,
                    isPlaying = uiState.isPlaying,
                    isBuffering = uiState.isBuffering,
                    positionMs = uiState.playbackPositionMs,
                    durationMs = uiState.durationMs,
                    isSaved = uiState.currentTrack?.let { viewModel.isVideoSaved(it.id) } ?: false,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNextTrack() },
                    onToggleSave = { uiState.currentTrack?.let { viewModel.toggleSaveVideo(it) } },
                    onClick = { viewModel.openNowPlayingSheet(true) }
                )

                // 4-Tab Bottom Navigation Bar (Home, Search, Library, Profile)
                MusicBottomBar(
                    currentTab = uiState.currentTab,
                    queueCount = uiState.queue.size,
                    onTabSelected = { viewModel.onSelectTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Active Tab Content
            when (uiState.currentTab) {
                0 -> MusicDiscoverScreen(uiState = uiState, viewModel = viewModel)
                1 -> MusicExploreScreen(uiState = uiState, viewModel = viewModel)
                2 -> MusicLibraryScreen(uiState = uiState, viewModel = viewModel)
                3 -> MusicProfileScreen(uiState = uiState, viewModel = viewModel)
            }
        }
    }

    // Full-Screen Now Playing Sheet Modal (Screen 4)
    if (uiState.isNowPlayingSheetOpen && uiState.currentTrack != null) {
        NowPlayingSheet(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.openNowPlayingSheet(false) }
        )
    }

    // Interactive Lyrics Sheet Modal (Screen 15)
    if (uiState.isLyricsSheetOpen && uiState.currentTrack != null) {
        LyricsSheet(
            track = uiState.currentTrack!!,
            onDismiss = { viewModel.openLyricsSheet(false) }
        )
    }

    // Queue / Up Next Sheet Modal (Screen 6)
    if (uiState.isQueueSheetOpen) {
        QueueBottomSheet(
            queue = uiState.queue,
            currentIndex = uiState.queueIndex,
            isPlaying = uiState.isPlaying,
            viewModel = viewModel,
            onDismiss = { viewModel.openQueueSheet(false) }
        )
    }

    // Album Detail Sheet (Screen 7)
    uiState.selectedAlbumDetail?.let { album ->
        AlbumDetailSheet(
            album = album,
            viewModel = viewModel,
            onDismiss = { viewModel.openAlbumDetail(null) }
        )
    }

    // Artist Profile Sheet (Screen 9)
    uiState.selectedArtistDetail?.let { artist ->
        ArtistProfileSheet(
            artist = artist,
            viewModel = viewModel,
            onDismiss = { viewModel.openArtistDetail(null) }
        )
    }

    // Sleep Timer Dialog (Screen 16)
    if (uiState.showSleepTimerDialog) {
        SleepTimerDialog(
            currentMinutes = uiState.sleepTimerMinutesRemaining,
            remainingSeconds = uiState.sleepTimerSecondsRemaining,
            onSelectMinutes = { viewModel.setSleepTimer(it) },
            onSelectEndOfTrack = { viewModel.setSleepTimer(null, endOfSong = true) },
            onDismiss = { viewModel.showSleepTimerDialog(false) }
        )
    }

    if (uiState.showListeningStatsDialog) {
        com.example.ui.components.ListeningStatsDialog(
            totalSeconds = uiState.totalListeningSeconds,
            mostPlayed = uiState.mostPlayedStats,
            topArtists = uiState.topArtistsStats,
            onDismiss = { viewModel.showListeningStatsDialog(false) }
        )
    }

    // Equalizer Dialog (Screen 17)
    if (uiState.showEqualizerDialog) {
        EqualizerDialog(
            currentPreset = uiState.equalizerPreset,
            band60 = uiState.band60Hz,
            band230 = uiState.band230Hz,
            band910 = uiState.band910Hz,
            band3600 = uiState.band3600Hz,
            band14000 = uiState.band14000Hz,
            isBassBoost = uiState.isBassBoost,
            isVirtualizer = uiState.isVirtualizer,
            onSelectPreset = { viewModel.setEqualizerPreset(it) },
            onBandChange = { index, value -> viewModel.setFrequencyBand(index, value) },
            onToggleBassBoost = { viewModel.toggleBassBoost() },
            onToggleVirtualizer = { viewModel.toggleVirtualizer() },
            onDismiss = { viewModel.showEqualizerDialog(false) }
        )
    }

    // Playback Settings Dialog (Screen 18)
    if (uiState.showPlaybackSettingsDialog) {
        PlaybackSettingsDialog(
            crossfadeSeconds = uiState.crossfadeSeconds,
            isGapless = uiState.isGaplessPlayback,
            isAutoplay = uiState.isAutoplay,
            isNormalize = uiState.isNormalizeVolume,
            isPlayInOrder = uiState.isPlayInOrder,
            onSetCrossfade = { viewModel.setCrossfade(it) },
            onToggleGapless = { viewModel.toggleGapless() },
            onToggleAutoplay = { viewModel.toggleAutoplay() },
            onToggleNormalize = { viewModel.toggleNormalizeVolume() },
            onTogglePlayInOrder = { viewModel.togglePlayInOrder() },
            onDismiss = { viewModel.showPlaybackSettingsDialog(false) }
        )
    }

    // Audio Quality Dialog (Screen 19)
    if (uiState.showAudioQualityDialog) {
        AudioQualityDialog(
            streamingQuality = uiState.streamingQuality,
            downloadQuality = uiState.downloadQuality,
            onSetStreamingQuality = { viewModel.setStreamingQuality(it) },
            onSetDownloadQuality = { viewModel.setDownloadQuality(it) },
            onDismiss = { viewModel.showAudioQualityDialog(false) }
        )
    }

    // Notifications Dialog (Screen 20)
    if (uiState.showNotificationsDialog) {
        NotificationsDialog(
            activities = uiState.activityEvents,
            onClearActivities = { viewModel.clearActivityEvents() },
            onDismiss = { viewModel.showNotificationsDialog(false) }
        )
    }

    // Appearance Theme Dialog (Screen 22)
    if (uiState.showAppearanceDialog) {
        AppearanceDialog(
            currentTheme = uiState.selectedTheme,
            onSelectTheme = { viewModel.setTheme(it) },
            onDismiss = { viewModel.showAppearanceDialog(false) }
        )
    }

    // Playback Speed Dialog
    if (uiState.showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = uiState.playbackSpeed,
            onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
            onDismiss = { viewModel.showSpeedDialog(false) }
        )
    }

    // Create Playlist Dialog
    if (uiState.showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onCreatePlaylist = { title, desc -> viewModel.createPlaylist(title, desc) },
            onDismiss = { viewModel.showCreatePlaylistDialog(false) }
        )
    }

    // Add To Playlist Dialog
    uiState.trackForPlaylistSelection?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = uiState.playlists,
            onSelectPlaylist = { plId -> viewModel.addTrackToPlaylist(plId, track) },
            onCreateNew = { viewModel.showCreatePlaylistDialog(true) },
            onDismiss = { viewModel.showAddToPlaylistDialog(null) }
        )
    }

    // Storage & Cache Dialog
    if (uiState.showStorageCacheDialog) {
        StorageCacheDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showStorageCacheDialog(false) }
        )
    }
}
