package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MusicTrackRow
import com.example.ui.components.QueueTrackItem
import com.example.ui.theme.ActivePillBg
import com.example.ui.theme.MusicPrimary
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftSurfaceGray
import com.example.ui.viewmodel.MusicPlayerUiState
import com.example.ui.viewmodel.VideoSearchViewModel

@Composable
fun MusicQueueScreen(
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("music_queue_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Playing Queue",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${uiState.queue.size} songs up next",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.queue.size > 1) {
                TextButton(
                    onClick = { viewModel.clearQueue() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MusicPrimary)
                ) {
                    Icon(Icons.Filled.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All")
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (uiState.queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your queue is currently empty",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Search and play tracks to add them here",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.queue) { index, track ->
                    val isCurrent = index == uiState.queueIndex
                    QueueTrackItem(
                        track = track,
                        index = index + 1,
                        isCurrent = isCurrent,
                        isPlaying = uiState.isPlaying && isCurrent,
                        onClick = {
                            viewModel.playTrack(track, uiState.queue)
                        },
                        onRemove = {
                            viewModel.removeFromQueue(index)
                        }
                    )
                }

                // Related Songs Matching User Taste from YouTube
                if (uiState.relatedTasteSongs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFF0000).copy(alpha = 0.12f),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFFF0000),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "YouTube Mix",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF0000)
                                        )
                                    }
                                }
                                Text(
                                    text = "Recommended For You",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            TextButton(onClick = { viewModel.addAllRelatedSongsToQueue() }) {
                                Icon(Icons.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(uiState.relatedTasteSongs) { relatedTrack ->
                        val isDl = viewModel.isSongDownloaded(relatedTrack.id)
                        val isDling = viewModel.isSongDownloading(relatedTrack.id)
                        val dlProg = viewModel.getDownloadProgress(relatedTrack.id)

                        MusicTrackRow(
                            track = relatedTrack,
                            isCurrentPlaying = false,
                            isPlaying = false,
                            isSaved = viewModel.isVideoSaved(relatedTrack.id),
                            isDownloaded = isDl,
                            isDownloading = isDling,
                            downloadProgress = dlProg,
                            onTrackClick = {
                                viewModel.playTrack(relatedTrack)
                            },
                            onPlayNext = {
                                viewModel.playRelatedSongNext(relatedTrack)
                            },
                            onAddToQueue = {
                                viewModel.addRelatedSongToQueue(relatedTrack)
                            },
                            onAddToPlaylist = {
                                viewModel.showAddToPlaylistDialog(relatedTrack)
                            },
                            onToggleSave = {
                                viewModel.toggleSaveVideo(relatedTrack)
                            },
                            onDownload = {
                                viewModel.downloadTrack(relatedTrack)
                            },
                            onDeleteDownload = {
                                viewModel.deleteDownloadedTrack(relatedTrack.id)
                            }
                        )
                    }
                } else if (uiState.isFetchingRelatedSongs) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MusicPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Finding songs that fit your taste on YouTube...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
