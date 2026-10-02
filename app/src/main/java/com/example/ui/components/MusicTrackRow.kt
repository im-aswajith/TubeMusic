package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.ActivePillBg
import com.example.ui.theme.MediumGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import java.util.Locale

/**
 * Music Track Row displaying the actual video thumbnail, animated equalizer for active track,
 * track title, channel/artist name, duration, and contextual action menu.
 */
@Composable
fun MusicTrackRow(
    track: YouTubeVideo,
    index: Int = 0,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    isSaved: Boolean,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float? = null,
    onTrackClick: (YouTubeVideo) -> Unit,
    onPlayNext: (YouTubeVideo) -> Unit,
    onAddToQueue: (YouTubeVideo) -> Unit,
    onAddToPlaylist: (YouTubeVideo) -> Unit,
    onToggleSave: (YouTubeVideo) -> Unit,
    onDownload: ((YouTubeVideo) -> Unit)? = null,
    onDeleteDownload: ((YouTubeVideo) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isMenuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onTrackClick(track) }
            .testTag("track_row_${track.id}"),
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrentPlaying) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left: Actual Video Thumbnail with Playing State Overlay
            val reliableThumb = remember(track.thumbnailUrl, track.id) {
                if (track.thumbnailUrl.isNotBlank()) track.thumbnailUrl
                else if (track.id.isNotBlank()) "https://i.ytimg.com/vi/${track.id}/hqdefault.jpg"
                else ""
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )

                if (reliableThumb.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(reliableThumb)
                            .crossfade(true)
                            .build(),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // If currently playing, show an equalizer or play badge overlay
                if (isCurrentPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying) {
                            MiniVisualizerBars(color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Playing",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Track Title & Artist / Duration
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.displayArtist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!track.duration.isNullOrBlank()) {
                        Text(
                            text = " • ${track.duration}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    } else if (track.durationSeconds > 0) {
                        val m = track.durationSeconds / 60
                        val s = track.durationSeconds % 60
                        Text(
                            text = String.format(Locale.getDefault(), " • %d:%02d", m, s),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    if (isDownloaded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1DB954)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDownward,
                                contentDescription = "Downloaded offline",
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    } else if (isDownloading) {
                        Spacer(modifier = Modifier.width(6.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.8.dp,
                            color = Color(0xFF1DB954)
                        )
                    }
                }
            }

            // 3. Right: Quick Download action & Three Dots Action Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloading) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val prog = (downloadProgress ?: 0.5f).coerceIn(0.05f, 1f)
                        CircularProgressIndicator(
                            progress = { prog },
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.4.dp,
                            color = Color(0xFF1DB954),
                            trackColor = Color(0xFF1DB954).copy(alpha = 0.2f)
                        )
                        Text(
                            text = "${(prog * 100).toInt()}%",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1DB954)
                        )
                    }
                } else if (isDownloaded) {
                    IconButton(
                        onClick = { onDeleteDownload?.invoke(track) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1DB954)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDownward,
                                contentDescription = "Downloaded offline",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                } else if (onDownload != null) {
                    IconButton(
                        onClick = { onDownload(track) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download song",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { isMenuOpen = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = "Track Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    if (isDownloaded) {
                        DropdownMenuItem(
                            text = { Text("Delete Download") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFDC2626)) },
                            onClick = {
                                isMenuOpen = false
                                onDeleteDownload?.invoke(track)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Download High Quality") },
                            leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null, tint = Color(0xFF1DB954)) },
                            onClick = {
                                isMenuOpen = false
                                onDownload?.invoke(track)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Play Next") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                        onClick = {
                            isMenuOpen = false
                            onPlayNext(track)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                        onClick = {
                            isMenuOpen = false
                            onAddToQueue(track)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            isMenuOpen = false
                            onAddToPlaylist(track)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isSaved) "Remove from Liked" else "Add to Liked") },
                        leadingIcon = {
                            Icon(
                                if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isSaved) Color(0xFFFF2D55) else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            isMenuOpen = false
                            onToggleSave(track)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                        onClick = {
                            isMenuOpen = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, track.title)
                                putExtra(Intent.EXTRA_TEXT, "${track.title}\nhttps://youtu.be/${track.id}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                        }
                    )
                }
            }
        }
    }
}
}
