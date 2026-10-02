package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PlayerDisplayMode
import com.example.data.model.RepeatMode
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.MediumGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurfaceGray
import com.example.ui.viewmodel.MusicPlayerUiState
import com.example.ui.viewmodel.VideoSearchViewModel

/**
 * Screen 4: Now Playing Screen matching designv2-example.png
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = uiState.currentTrack ?: return
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isMenuExpanded by remember { mutableStateOf(false) }

    val totalMs = if (uiState.durationMs > 0) uiState.durationMs else (track.durationSeconds * 1000L).coerceAtLeast(1000L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CanvasBg,
        dragHandle = null,
        modifier = modifier.fillMaxSize().testTag("now_playing_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasBg)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // 1. Top Header: Collapse Chevron, "Now Playing", 3-dots Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("collapse_now_playing")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Now Playing",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack,
                    letterSpacing = (-0.3).sp
                )

                Box {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.testTag("now_playing_more_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = "More Options",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sleep Timer (${uiState.sleepTimerMinutesRemaining ?: "Off"})") },
                            leadingIcon = { Icon(Icons.Filled.Nightlight, contentDescription = null) },
                            onClick = {
                                isMenuExpanded = false
                                viewModel.showSleepTimerDialog(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Equalizer (${uiState.equalizerPreset.label})") },
                            leadingIcon = { Icon(Icons.Filled.Equalizer, contentDescription = null) },
                            onClick = {
                                isMenuExpanded = false
                                viewModel.showEqualizerDialog(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Playback Speed (${uiState.playbackSpeed}x)") },
                            leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null) },
                            onClick = {
                                isMenuExpanded = false
                                viewModel.showSpeedDialog(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            leadingIcon = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
                            onClick = {
                                isMenuExpanded = false
                                viewModel.showAddToPlaylistDialog(track)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (uiState.playerDisplayMode == PlayerDisplayMode.AUDIO_ARTWORK)
                                        "Switch to Video Mode"
                                    else
                                        "Switch to Audio Artwork"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (uiState.playerDisplayMode == PlayerDisplayMode.AUDIO_ARTWORK)
                                        Icons.Filled.Movie
                                    else
                                        Icons.Filled.Headphones,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                val nextMode = if (uiState.playerDisplayMode == PlayerDisplayMode.AUDIO_ARTWORK)
                                    PlayerDisplayMode.VIDEO_STREAM
                                else
                                    PlayerDisplayMode.AUDIO_ARTWORK
                                viewModel.setPlayerDisplayMode(nextMode)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Track") },
                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                            onClick = {
                                isMenuExpanded = false
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Listen to \"${track.title}\" on Vibe Music:\n${track.watchUrl}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Track"))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Large Album Artwork Card (24dp rounded corners with soft drop shadow)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = Color(0x33000000),
                        spotColor = Color(0x33000000)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(track.maxResThumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (uiState.isBuffering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x55000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PureWhite, strokeWidth = 3.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Track Info Row: Title & Artist on Left, Favorite Heart on Right
            val isSaved = viewModel.isVideoSaved(track.id)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isDownloaded = viewModel.isSongDownloaded(track.id)
                val isDownloading = viewModel.isSongDownloading(track.id)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = track.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = track.displayArtist,
                            fontSize = 15.sp,
                            color = MediumGray,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Audio Quality Indicator Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SoftSurfaceGray,
                            modifier = Modifier.clickable { viewModel.showAudioQualityDialog(true) }
                        ) {
                            Text(
                                text = if (isDownloaded) "OFFLINE • 320 KBPS" else uiState.streamingQuality.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDownloaded) Color(0xFF1DB954) else ObsidianBlack,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Spotify-style Download Button
                    IconButton(
                        onClick = {
                            if (isDownloaded) {
                                viewModel.deleteDownloadedTrack(track.id)
                            } else if (isDownloading) {
                                viewModel.cancelDownload(track.id)
                            } else {
                                viewModel.downloadTrack(track)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("now_playing_download_btn")
                    ) {
                        if (isDownloading) {
                            val downloadProg = (uiState.downloadProgressMap[track.id] ?: 0.5f).coerceIn(0.05f, 1f)
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { downloadProg },
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.4.dp,
                                    color = Color(0xFF1DB954),
                                    trackColor = Color(0xFF1DB954).copy(alpha = 0.2f)
                                )
                                Text(
                                    text = "${(downloadProg * 100).toInt()}%",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1DB954)
                                )
                            }
                        } else if (isDownloaded) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1DB954)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDownward,
                                    contentDescription = "Downloaded offline",
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download song",
                                tint = MediumGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { viewModel.toggleSaveVideo(track) },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("now_playing_heart_btn")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Save track",
                            tint = if (isSaved) ObsidianBlack else MediumGray,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Interactive Audio Waveform Scrubber
            AudioWaveformScrubber(
                currentPositionMs = uiState.playbackPositionMs,
                totalDurationMs = totalMs,
                isPlaying = uiState.isPlaying,
                isLive = track.isLive,
                onSeekTo = { targetMs -> viewModel.seekTo(targetMs) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Playback Controls Row: Shuffle | Prev | Play/Pause (72dp) | Next | Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Shuffle Button
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (uiState.isShuffleEnabled) ObsidianBlack else MediumGray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Previous Button
                IconButton(
                    onClick = { viewModel.playPreviousTrack() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("prev_track_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Giant Circular Play/Pause Button (72dp Obsidian Black)
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            ambientColor = Color(0x33000000)
                        )
                        .clip(CircleShape)
                        .clickable { viewModel.togglePlayPause() }
                        .testTag("main_play_pause_btn"),
                    shape = CircleShape,
                    color = ObsidianBlack
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (uiState.isBuffering) {
                            CircularProgressIndicator(
                                color = PureWhite,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = PureWhite,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Next Button
                IconButton(
                    onClick = { viewModel.playNextTrack() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_track_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next Track",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Repeat Button
                IconButton(
                    onClick = { viewModel.toggleRepeatMode() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = when (uiState.repeatMode) {
                            RepeatMode.ONE -> Icons.Filled.RepeatOne
                            RepeatMode.ALL -> Icons.Filled.Repeat
                            RepeatMode.OFF -> Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat Mode",
                        tint = if (uiState.repeatMode != RepeatMode.OFF) ObsidianBlack else MediumGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 6. Bottom Action Pill Buttons: "Lyrics" & "Queue"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = { viewModel.openLyricsSheet(true) },
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftSurfaceGray,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("lyrics_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lyrics,
                        contentDescription = "Lyrics",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lyrics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = { viewModel.openQueueSheet(true) },
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftSurfaceGray,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("queue_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Queue (${uiState.queue.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
