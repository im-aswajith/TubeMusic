package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.data.model.Playlist
import com.example.data.model.YouTubeVideo
import com.example.ui.components.MusicTrackRow
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.MediumGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurfaceGray
import com.example.ui.viewmodel.MusicPlayerUiState
import com.example.ui.viewmodel.VideoSearchViewModel

/**
 * Screen 11 & 14: Library & Liked Songs matching designv2-example.png
 */
@Composable
fun MusicLibraryScreen(
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Playlists") }
    val filters = listOf("Playlists", "Albums", "Artists", "Downloads")

    if (uiState.isLikedSongsScreenOpen) {
        // Liked Songs Screen (Screen 14)
        LikedSongsView(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { viewModel.openLikedSongsScreen(false) }
        )
        return
    }

    if (uiState.selectedPlaylist != null) {
        // Playlist Detail View
        PlaylistDetailView(
            playlist = uiState.selectedPlaylist,
            uiState = uiState,
            viewModel = viewModel,
            onBack = { viewModel.selectPlaylist(null) }
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
            .testTag("music_library_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header: "Your Library" & Action Icons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Library",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ObsidianBlack,
                    letterSpacing = (-0.5).sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.onSelectTab(1) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Library",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.showCreatePlaylistDialog(true) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create Playlist",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 2. Filter Chips: Playlists, Albums, Artists, Downloads
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) ObsidianBlack else SoftSurfaceGray,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) PureWhite else ObsidianBlack,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. System Playlists: Liked Songs & Recently Added
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Liked Songs Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SoftSurfaceGray,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.openLikedSongsScreen(true) }
                        .testTag("liked_songs_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Liked",
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Liked Songs",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                        Text(
                            text = "${uiState.savedVideos.size} songs",
                            fontSize = 12.sp,
                            color = MediumGray
                        )
                    }
                }

                // Downloads Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SoftSurfaceGray,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectedFilter = "Downloads" }
                        .testTag("downloads_library_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (uiState.downloadedVideos.isNotEmpty()) Color(0xFF1DB954) else CanvasBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Downloads",
                                tint = if (uiState.downloadedVideos.isNotEmpty()) PureWhite else ObsidianBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Downloads",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                        Text(
                            text = "${uiState.downloadedVideos.size} tracks",
                            fontSize = 12.sp,
                            color = MediumGray
                        )
                    }
                }
            }
        }

        // When Downloads filter is selected
        if (selectedFilter == "Downloads") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Downloaded Music",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                        Text(
                            text = "${uiState.downloadedVideos.size} songs • ${viewModel.getDownloadedStorageSize()}",
                            fontSize = 12.sp,
                            color = MediumGray
                        )
                    }

                    Text(
                        text = "Manage Cache",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1DB954),
                        modifier = Modifier.clickable { viewModel.showStorageCacheDialog(true) }
                    )
                }
            }

            if (uiState.downloadedVideos.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SoftSurfaceGray,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = MediumGray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No offline downloads yet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap ⋮ next to any track to download it at high quality for offline playback.", color = MediumGray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                items(uiState.downloadedVideos) { dt ->
                    val track = dt.video
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
        } else {
            // 4. Playlists Header & Add Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playlists",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack,
                        letterSpacing = (-0.3).sp
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SoftSurfaceGray,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.showCreatePlaylistDialog(true) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = ObsidianBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianBlack)
                        }
                    }
                }
            }

            // Playlists Items
            if (uiState.playlists.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SoftSurfaceGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showCreatePlaylistDialog(true) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CanvasBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = MediumGray)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Create your first playlist", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Save your favorite songs in custom lists", color = MediumGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(uiState.playlists) { playlist ->
                    PlaylistRowItem(
                        playlist = playlist,
                        onClick = { viewModel.selectPlaylist(playlist) },
                        onDelete = { viewModel.deletePlaylist(playlist.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun LikedSongsView(
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ObsidianBlack
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Liked Songs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
            }
        }

        // Shuffle Play Pill Button
        item {
            Button(
                onClick = {
                    if (uiState.savedVideos.isNotEmpty()) {
                        viewModel.toggleShuffle()
                        viewModel.playTrack(uiState.savedVideos.random(), uiState.savedVideos)
                    }
                },
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianBlack,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shuffle Play (${uiState.savedVideos.size} songs)", fontWeight = FontWeight.Bold)
            }
        }

        if (uiState.savedVideos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No liked songs yet. Tap heart on any track to save it!", color = MediumGray)
                }
            }
        } else {
            itemsIndexed(uiState.savedVideos) { index, track ->
                MusicTrackRow(
                    track = track,
                    index = index,
                    isCurrentPlaying = uiState.currentTrack?.id == track.id,
                    isPlaying = uiState.isPlaying,
                    isSaved = true,
                    onTrackClick = { viewModel.playTrack(track, uiState.savedVideos) },
                    onPlayNext = { viewModel.playNextInQueue(track) },
                    onAddToQueue = { viewModel.addToQueue(track) },
                    onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                    onToggleSave = { viewModel.toggleSaveVideo(track) }
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activePlaylist = remember(playlist, uiState.playlists) {
        uiState.playlists.find { it.id == playlist.id } ?: playlist
    }
    // Only songs manually added to the playlist are displayed
    val playlistVideos = activePlaylist.tracks

    val playlistThumb = remember(activePlaylist) {
        val first = activePlaylist.tracks.firstOrNull()
        if (first != null) {
            if (first.thumbnailUrl.isNotBlank()) first.thumbnailUrl
            else if (first.id.isNotBlank()) "https://i.ytimg.com/vi/${first.id}/hqdefault.jpg"
            else null
        } else {
            activePlaylist.coverUrl
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ObsidianBlack
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activePlaylist.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val countText = if (playlistVideos.size == 1) "1 song" else "${playlistVideos.size} songs"
                    Text(
                        text = if (activePlaylist.description.isNotBlank()) "${activePlaylist.description} • $countText" else countText,
                        fontSize = 12.sp,
                        color = MediumGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Playlist Thumbnail Banner (if songs exist or cover exists)
        if (playlistVideos.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SoftSurfaceGray,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CanvasBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!playlistThumb.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(playlistThumb)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = activePlaylist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = ObsidianBlack,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activePlaylist.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${playlistVideos.size} songs saved",
                                fontSize = 13.sp,
                                color = MediumGray
                            )
                        }
                    }
                }
            }

            // Play & Shuffle Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (playlistVideos.isNotEmpty()) {
                                viewModel.playTrack(playlistVideos.first(), playlistVideos)
                            }
                        },
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianBlack,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play All", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (playlistVideos.isNotEmpty()) {
                                viewModel.toggleShuffle()
                                viewModel.playTrack(playlistVideos.random(), playlistVideos)
                            }
                        },
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftSurfaceGray,
                            contentColor = ObsidianBlack
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shuffle", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // List of songs in the playlist
            itemsIndexed(playlistVideos) { index, track ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MusicTrackRow(
                            track = track,
                            index = index,
                            isCurrentPlaying = uiState.currentTrack?.id == track.id,
                            isPlaying = uiState.isPlaying,
                            isSaved = viewModel.isVideoSaved(track.id),
                            onTrackClick = { viewModel.playTrack(track, playlistVideos) },
                            onPlayNext = { viewModel.playNextInQueue(track) },
                            onAddToQueue = { viewModel.addToQueue(track) },
                            onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                            onToggleSave = { viewModel.toggleSaveVideo(track) }
                        )
                    }
                    IconButton(
                        onClick = { viewModel.removeTrackFromPlaylist(activePlaylist.id, track.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Remove from playlist",
                            tint = MediumGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            // Empty State: Do not show anything until songs are manually added
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SoftSurfaceGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MediumGray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Playlist is Empty",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No songs in this playlist yet. Add songs manually from Search or Discover by tapping the ⋮ menu and selecting \"Add to Playlist\".",
                        fontSize = 13.sp,
                        color = MediumGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistRowItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val songThumb = remember(playlist) {
        val first = playlist.tracks.firstOrNull()
        if (first != null) {
            if (first.thumbnailUrl.isNotBlank()) first.thumbnailUrl
            else if (first.id.isNotBlank()) "https://i.ytimg.com/vi/${first.id}/hqdefault.jpg"
            else null
        } else {
            playlist.coverUrl
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SoftSurfaceGray,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CanvasBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (!songThumb.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(songThumb)
                                .crossfade(true)
                                .build(),
                            contentDescription = playlist.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = ObsidianBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = playlist.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val count = playlist.tracks.size
                    val countText = if (count == 1) "1 song" else "$count songs"
                    Text(
                        text = countText,
                        fontSize = 12.sp,
                        color = MediumGray
                    )
                }
            }

            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "Options",
                        tint = MediumGray
                    )
                }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Playlist", color = Color(0xFFDC2626)) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Color(0xFFDC2626)) },
                        onClick = {
                            isMenuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
