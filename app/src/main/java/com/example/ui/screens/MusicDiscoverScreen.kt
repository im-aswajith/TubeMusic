package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.YouTubeVideo
import com.example.ui.components.MusicTrackRow
import com.example.ui.theme.ActivePillBg
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.DarkCharcoal
import com.example.ui.theme.LightBorderGray
import com.example.ui.theme.MediumGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurfaceGray
import com.example.ui.viewmodel.ArtistProfile
import com.example.ui.viewmodel.MusicPlayerUiState
import com.example.ui.viewmodel.VideoSearchViewModel
import java.util.Calendar

/**
 * Spotify Home Screen UI/UX matching Spotify's modern layout:
 * - Dynamic Time Greeting & User Avatar Header with Settings & History
 * - Quick Filter Pills: All, Music, Podcasts, Downloaded (Offline)
 * - Signature 2-Column Quick Access Grid (Liked Songs, Daily Mixes, Offline Downloads)
 * - Horizontal Carousels: Jump Back In, Daily Mixes, Offline Downloaded, Top Hits, Artists
 * - Full High-Quality Offline Download and Storage Management
 */
@Composable
fun MusicDiscoverScreen(
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val featuredTracks = if (uiState.quickPicks.isNotEmpty()) uiState.quickPicks else uiState.searchResults
    val heroTrack = featuredTracks.firstOrNull() ?: uiState.currentTrack

    // Calculate dynamic time of day greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..22 -> "Good evening"
            else -> "Good night"
        }
    }

    val activeCategory = uiState.homeFilterCategory
    val downloadedCount = uiState.downloadedVideos.size

    val realArtists = remember(uiState.recentlyPlayed, uiState.trendingTracks, uiState.quickPicks) {
        viewModel.getArtistsBasedOnRecentlyPlayed()
    }
    val activeArtist = remember(realArtists) { realArtists.firstOrNull() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
            .testTag("spotify_home_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Top Header: Dynamic Time Greeting (Profile picture removed per request), Activity History, Notifications, Storage/Settings Gear
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ObsidianBlack,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Header Action Icons (Notifications, History, Settings/Cache)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Activity History Clock
                    IconButton(
                        onClick = { viewModel.showNotificationsDialog(true) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SoftSurfaceGray)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "Listening History",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Notification Bell
                    IconButton(
                        onClick = { viewModel.showNotificationsDialog(true) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SoftSurfaceGray)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = ObsidianBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            if (uiState.activityEvents.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1DB954))
                                )
                            }
                        }
                    }

                    // Settings / Storage Cache Gear
                    IconButton(
                        onClick = { viewModel.showStorageCacheDialog(true) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SoftSurfaceGray)
                            .testTag("home_settings_gear_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Storage & Settings",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Spotify Pill Filter Chips: "All", "Music", "Podcasts", "Downloaded"
        item {
            val filterChips = listOf("All", "Music", "Podcasts", "Downloaded")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterChips) { chip ->
                    val isSelected = activeCategory == chip
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.setHomeFilterCategory(chip) }
                            .testTag("filter_chip_$chip"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) ObsidianBlack else SoftSurfaceGray
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (chip == "Downloaded") {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF1DB954) else Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (chip == "Downloaded" && downloadedCount > 0) "Downloaded ($downloadedCount)" else chip,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PureWhite else ObsidianBlack
                            )
                        }
                    }
                }
            }
        }

        // Modern Download Progress Animation Banner
        if (uiState.downloadingIds.isNotEmpty()) {
            item {
                val downloadingId = uiState.downloadingIds.firstOrNull() ?: ""
                val progress = (uiState.downloadProgressMap[downloadingId] ?: 0.5f).coerceIn(0.05f, 1f)
                val targetTrack = (uiState.searchResults + uiState.recentlyPlayed + uiState.trendingTracks + uiState.quickPicks)
                    .firstOrNull { it.id == downloadingId } ?: uiState.currentTrack

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianBlack
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF27272A)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.4.dp,
                                color = Color(0xFF1DB954),
                                trackColor = Color(0xFF3F3F46)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (targetTrack != null) "Downloading \"${targetTrack.title}\"..." else "Downloading for offline...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF1DB954),
                                trackColor = Color(0xFF3F3F46)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1DB954)
                        )
                    }
                }
            }
        }

        // 3. Spotify Signature 2-Column Quick Access Grid (6 Items)
        if (activeCategory == "All" || activeCategory == "Music") {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Row 1: Liked Songs & Daily Mix 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpotifyQuickAccessCard(
                            title = "Liked Songs",
                            subtitle = "${uiState.savedVideos.size} songs",
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF450AF5), Color(0xFFC4EFD9))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            isPlaying = uiState.isPlaying && uiState.currentTrack != null && uiState.savedVideos.any { it.id == uiState.currentTrack?.id },
                            onClick = {
                                if (uiState.savedVideos.isNotEmpty()) {
                                    viewModel.playTrack(uiState.savedVideos.first(), uiState.savedVideos)
                                } else {
                                    viewModel.openLikedSongsScreen(true)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        SpotifyQuickAccessCard(
                            title = "Daily Mix 1",
                            subtitle = "Pop & Dance",
                            imageUrl = featuredTracks.firstOrNull()?.thumbnailUrl,
                            isPlaying = false,
                            onClick = {
                                if (featuredTracks.isNotEmpty()) {
                                    viewModel.playTrack(featuredTracks.first(), featuredTracks)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Offline Downloaded & Discover Weekly
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpotifyQuickAccessCard(
                            title = "Downloaded",
                            subtitle = if (downloadedCount > 0) "$downloadedCount tracks" else "Offline ready",
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF0F766E), Color(0xFF1DB954))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DownloadDone,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            },
                            isPlaying = uiState.isPlaying && uiState.currentTrack != null && viewModel.isSongDownloaded(uiState.currentTrack!!.id),
                            onClick = {
                                if (uiState.downloadedVideos.isNotEmpty()) {
                                    val downloadedTracks = uiState.downloadedVideos.map { it.video }
                                    viewModel.playTrack(downloadedTracks.first(), downloadedTracks)
                                } else {
                                    viewModel.setHomeFilterCategory("Downloaded")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        val secondTrack = featuredTracks.getOrNull(1) ?: heroTrack
                        SpotifyQuickAccessCard(
                            title = "Discover Weekly",
                            subtitle = "Fresh for you",
                            imageUrl = secondTrack?.thumbnailUrl,
                            isPlaying = false,
                            onClick = {
                                if (featuredTracks.size > 1) {
                                    viewModel.playTrack(featuredTracks[1], featuredTracks)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 3: Top Hits 2024 & Chill Vibes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val thirdTrack = featuredTracks.getOrNull(2) ?: heroTrack
                        SpotifyQuickAccessCard(
                            title = "Top Hits",
                            subtitle = "Trending charts",
                            imageUrl = thirdTrack?.thumbnailUrl,
                            isPlaying = false,
                            onClick = {
                                if (featuredTracks.size > 2) {
                                    viewModel.playTrack(featuredTracks[2], featuredTracks)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        val fourthTrack = featuredTracks.getOrNull(3) ?: heroTrack
                        SpotifyQuickAccessCard(
                            title = "Chill Vibes",
                            subtitle = "Acoustic & Lofi",
                            imageUrl = fourthTrack?.thumbnailUrl,
                            isPlaying = false,
                            onClick = {
                                if (featuredTracks.size > 3) {
                                    viewModel.playTrack(featuredTracks[3], featuredTracks)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Downloaded Offline View (When "Downloaded" filter is clicked OR when user has downloads)
        if (activeCategory == "Downloaded") {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Storage Banner Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = ObsidianBlack
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Offline Music Library",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = "$downloadedCount songs ready • ${viewModel.getDownloadedStorageSize()}",
                                        fontSize = 13.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.showStorageCacheDialog(true) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF27272A))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Manage Storage",
                                        tint = PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (downloadedCount > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val list = uiState.downloadedVideos.map { it.video }
                                            viewModel.playTrack(list.first(), list)
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1DB954),
                                            contentColor = PureWhite
                                        ),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play Offline", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val list = uiState.downloadedVideos.map { it.video }.shuffled()
                                            viewModel.playTrack(list.first(), list)
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF27272A),
                                            contentColor = PureWhite
                                        ),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Shuffle", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.downloadedVideos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(SoftSurfaceGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDownward,
                                    contentDescription = null,
                                    tint = MediumGray,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No downloaded tracks yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the ⋮ menu on any song and choose \"Download High Quality\" to play offline anytime.",
                                fontSize = 13.sp,
                                color = MediumGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                items(uiState.downloadedVideos) { downloaded ->
                    val track = downloaded.video
                    MusicTrackRow(
                        track = track,
                        isCurrentPlaying = uiState.currentTrack?.id == track.id,
                        isPlaying = uiState.isPlaying,
                        isSaved = viewModel.isVideoSaved(track.id),
                        isDownloaded = true,
                        isDownloading = false,
                        onTrackClick = {
                            val list = uiState.downloadedVideos.map { it.video }
                            viewModel.playTrack(track, list)
                        },
                        onPlayNext = { viewModel.playNextInQueue(track) },
                        onAddToQueue = { viewModel.addToQueue(track) },
                        onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                        onToggleSave = { viewModel.toggleSaveVideo(track) },
                        onDeleteDownload = { viewModel.deleteDownloadedTrack(track.id) }
                    )
                }
            }
        }

        // 5. Jump Back In / Recently Played Carousel
        if (activeCategory == "All" || activeCategory == "Music") {
            val recentList = if (uiState.recentlyPlayed.isNotEmpty()) {
                uiState.recentlyPlayed
            } else if (featuredTracks.size > 1) {
                featuredTracks.drop(1).take(6)
            } else {
                featuredTracks
            }

            if (recentList.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Jump back in",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "See all",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MediumGray,
                                modifier = Modifier.clickable { viewModel.onSelectTab(2) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(recentList) { track ->
                                val isDownloaded = viewModel.isSongDownloaded(track.id)
                                val isDownloading = viewModel.isSongDownloading(track.id)
                                val downloadProg = uiState.downloadProgressMap[track.id]
                                SpotifySquareCard(
                                    track = track,
                                    isDownloaded = isDownloaded,
                                    isDownloading = isDownloading,
                                    downloadProgress = downloadProg,
                                    onDownloadClick = {
                                        if (isDownloaded) {
                                            viewModel.deleteDownloadedTrack(track.id)
                                        } else {
                                            viewModel.downloadTrack(track)
                                        }
                                    },
                                    onClick = { viewModel.playTrack(track, recentList) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Spotify "Made for you" Daily Mixes
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Made for you",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val mixes = listOf(
                        Triple("Daily Mix 1", "The Weeknd, Dua Lipa, Post Malone", Color(0xFFEF4444)),
                        Triple("Daily Mix 2", "Billie Eilish, Olivia Rodrigo, Taylor Swift", Color(0xFF3B82F6)),
                        Triple("Daily Mix 3", "Kendrick Lamar, Travis Scott, Drake", Color(0xFF10B981)),
                        Triple("Daily Mix 4", "Coldplay, Imagine Dragons, OneRepublic", Color(0xFFF59E0B))
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(mixes) { idx, (mixTitle, artists, colorAccent) ->
                            val sampleTrack = featuredTracks.getOrNull(idx) ?: heroTrack
                            SpotifyDailyMixCard(
                                title = mixTitle,
                                artists = artists,
                                accentColor = colorAccent,
                                imageUrl = sampleTrack?.thumbnailUrl,
                                onClick = {
                                    if (featuredTracks.isNotEmpty()) {
                                        viewModel.playTrack(featuredTracks[idx % featuredTracks.size], featuredTracks)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 7. Downloaded Offline Carousel (Quick offline access if user has downloaded songs)
            if (uiState.downloadedVideos.isNotEmpty() && activeCategory != "Downloaded") {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1DB954)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDownward,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Downloaded Music",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianBlack,
                                    letterSpacing = (-0.3).sp
                                )
                            }

                            Text(
                                text = "View all ($downloadedCount)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MediumGray,
                                modifier = Modifier.clickable { viewModel.setHomeFilterCategory("Downloaded") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.downloadedVideos) { dt ->
                                SpotifySquareCard(
                                    track = dt.video,
                                    isDownloaded = true,
                                    onClick = {
                                        val list = uiState.downloadedVideos.map { it.video }
                                        viewModel.playTrack(dt.video, list)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 8. Real Artists & Their Songs Based On Listening History
            if (realArtists.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Artists You Recently Played",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack,
                                letterSpacing = (-0.3).sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(realArtists) { artist ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(86.dp)
                                        .clickable {
                                            viewModel.openArtistDetail(
                                                ArtistProfile(
                                                    name = artist.artistName,
                                                    imageUrl = artist.imageUrl,
                                                    songs = artist.songs
                                                )
                                            )
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(SoftSurfaceGray)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(artist.imageUrl).crossfade(true).build(),
                                            contentDescription = artist.artistName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = artist.artistName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ObsidianBlack,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${artist.songs.size} tracks",
                                        fontSize = 11.sp,
                                        color = MediumGray
                                    )
                                }
                            }
                        }
                    }
                }

                // Real Artist's Song List
                if (activeArtist != null && activeArtist.songs.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Songs by ${activeArtist.artistName}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianBlack
                                )
                                Text(
                                    text = "See all (${activeArtist.songs.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MediumGray,
                                    modifier = Modifier.clickable {
                                        viewModel.openArtistDetail(
                                            ArtistProfile(
                                                name = activeArtist.artistName,
                                                imageUrl = activeArtist.imageUrl,
                                                songs = activeArtist.songs
                                            )
                                        )
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    itemsIndexed(activeArtist.songs.take(4)) { index, track ->
                        val isDownloaded = viewModel.isSongDownloaded(track.id)
                        val isDownloading = viewModel.isSongDownloading(track.id)
                        val downloadProg = uiState.downloadProgressMap[track.id]

                        MusicTrackRow(
                            track = track,
                            index = index,
                            isCurrentPlaying = uiState.currentTrack?.id == track.id,
                            isPlaying = uiState.isPlaying,
                            isSaved = viewModel.isVideoSaved(track.id),
                            isDownloaded = isDownloaded,
                            isDownloading = isDownloading,
                            downloadProgress = downloadProg,
                            onTrackClick = { viewModel.playTrack(track, activeArtist.songs) },
                            onPlayNext = { viewModel.playNextInQueue(track) },
                            onAddToQueue = { viewModel.addToQueue(track) },
                            onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                            onToggleSave = { viewModel.toggleSaveVideo(track) },
                            onDownload = { viewModel.downloadTrack(track) },
                            onDeleteDownload = { viewModel.deleteDownloadedTrack(track.id) }
                        )
                    }
                }
            }

            // 9. Trending Songs Feed
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trending Songs",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack,
                        letterSpacing = (-0.3).sp
                    )
                }
            }

            if (uiState.isLoading && featuredTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ObsidianBlack, strokeWidth = 2.5.dp)
                    }
                }
            } else {
                itemsIndexed(featuredTracks.take(15)) { index, track ->
                    val isDownloaded = viewModel.isSongDownloaded(track.id)
                    val isDownloading = viewModel.isSongDownloading(track.id)
                    val downloadProg = uiState.downloadProgressMap[track.id]

                    MusicTrackRow(
                        track = track,
                        index = index,
                        isCurrentPlaying = uiState.currentTrack?.id == track.id,
                        isPlaying = uiState.isPlaying,
                        isSaved = viewModel.isVideoSaved(track.id),
                        isDownloaded = isDownloaded,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProg,
                        onTrackClick = { viewModel.playTrack(track, featuredTracks) },
                        onPlayNext = { viewModel.playNextInQueue(track) },
                        onAddToQueue = { viewModel.addToQueue(track) },
                        onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                        onToggleSave = { viewModel.toggleSaveVideo(track) },
                        onDownload = { viewModel.downloadTrack(track) },
                        onDeleteDownload = { viewModel.deleteDownloadedTrack(track.id) }
                    )
                }
            }
        }
    }
}

/**
 * Spotify Signature Quick Access Card (Left square artwork, bold title, right play/active indicator)
 */
@Composable
fun SpotifyQuickAccessCard(
    title: String,
    subtitle: String? = null,
    imageUrl: String? = null,
    icon: (@Composable () -> Unit)? = null,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag("quick_access_${title.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(8.dp),
        color = SoftSurfaceGray
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork / Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    icon()
                } else if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Headphones,
                        contentDescription = null,
                        tint = MediumGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MediumGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right play / active wave indicator
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1DB954)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "Playing",
                        tint = PureWhite,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Spotify Square Card (Large album/track card for carousels)
 */
@Composable
fun SpotifySquareCard(
    track: YouTubeVideo,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float? = null,
    onDownloadClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(135.dp)
                .shadow(elevation = 5.dp, shape = RoundedCornerShape(12.dp), ambientColor = Color(0x1A000000))
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE5E7EB))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(track.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top-right direct download button / progress animation
            if (isDownloading) {
                val prog = (downloadProgress ?: 0.5f).coerceIn(0.05f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { prog },
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF1DB954),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            } else if (onDownloadClick != null && !isDownloaded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { onDownloadClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Download song",
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Offline Download badge on card if downloaded
            if (isDownloaded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1DB954)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Downloaded",
                        tint = PureWhite,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ObsidianBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.displayArtist,
            fontSize = 11.sp,
            color = MediumGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Spotify Daily Mix Card with Color Bar Accent
 */
@Composable
fun SpotifyDailyMixCard(
    title: String,
    artists: String,
    accentColor: Color,
    imageUrl: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .width(145.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = SoftSurfaceGray
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFE5E7EB))
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Spotify Mix bottom color stripe
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(accentColor)
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = artists,
                    fontSize = 11.sp,
                    color = MediumGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
