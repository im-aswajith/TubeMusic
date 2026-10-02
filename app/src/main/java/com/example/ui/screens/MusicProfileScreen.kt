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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.LightBorderGray
import com.example.ui.theme.MediumGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurfaceGray
import com.example.ui.viewmodel.MusicPlayerUiState
import com.example.ui.viewmodel.VideoSearchViewModel

/**
 * Screen 22: Profile & Settings Screen matching designv2-example.png
 */
@Composable
fun MusicProfileScreen(
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
            .testTag("music_profile_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Profile Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = SoftSurfaceGray
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = "Music Profile",
                            tint = PureWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "My Music Hub",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "🎵", fontSize = 16.sp)
                        }
                        Text(
                            text = "Offline Ready • Private Session",
                            fontSize = 12.sp,
                            color = MediumGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianBlack
                        ) {
                            Text(
                                text = "HiFi Audio • Lossless 320kbps",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Audio & Playback Settings Section
        item {
            Text(
                text = "Audio & Playback",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ObsidianBlack,
                letterSpacing = (-0.2).sp
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SoftSurfaceGray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRowItem(
                        icon = Icons.Filled.Palette,
                        title = "Appearance",
                        subtitle = uiState.selectedTheme,
                        onClick = { viewModel.showAppearanceDialog(true) }
                    )
                    HorizontalDivider(color = LightBorderGray.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsRowItem(
                        icon = Icons.Filled.Headphones,
                        title = "Audio Quality",
                        subtitle = "${uiState.streamingQuality} streaming • ${uiState.downloadQuality}",
                        onClick = { viewModel.showAudioQualityDialog(true) }
                    )
                    HorizontalDivider(color = LightBorderGray.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsRowItem(
                        icon = Icons.Filled.PlayCircleOutline,
                        title = "Playback Settings",
                        subtitle = "Crossfade ${uiState.crossfadeSeconds}s • Gapless: ${if (uiState.isGaplessPlayback) "On" else "Off"}",
                        onClick = { viewModel.showPlaybackSettingsDialog(true) }
                    )
                    HorizontalDivider(color = LightBorderGray.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsRowItem(
                        icon = Icons.Filled.Equalizer,
                        title = "Equalizer",
                        subtitle = uiState.equalizerPreset.label,
                        onClick = { viewModel.showEqualizerDialog(true) }
                    )
                    HorizontalDivider(color = LightBorderGray.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsRowItem(
                        icon = Icons.Filled.Nightlight,
                        title = "Sleep Timer V2",
                        subtitle = if (uiState.sleepTimerMinutesRemaining != null) "${uiState.sleepTimerMinutesRemaining}m remaining (gentle fade)" else "Off",
                        onClick = { viewModel.showSleepTimerDialog(true) }
                    )
                    HorizontalDivider(color = LightBorderGray.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsRowItem(
                        icon = Icons.Filled.Insights,
                        title = "Listening Insights",
                        subtitle = "${uiState.totalListeningSeconds / 60} mins listened • Private statistics",
                        onClick = { viewModel.showListeningStatsDialog(true) }
                    )
                }
            }
        }

        // 3. Storage & Downloads Section
        item {
            Text(
                text = "Storage & Downloads",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ObsidianBlack,
                letterSpacing = (-0.2).sp
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SoftSurfaceGray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRowItem(
                        icon = Icons.Filled.Download,
                        title = "Downloaded Tracks",
                        subtitle = "${uiState.downloadedVideos.size} tracks stored offline",
                        onClick = { viewModel.onSelectTab(2) }
                    )
                    HorizontalDivider(color = LightBorderGray.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsRowItem(
                        icon = Icons.Filled.WifiOff,
                        title = "Offline Only Mode",
                        subtitle = if (uiState.isOfflineModeOnly) "Enabled • Showing local audio only" else "Disabled • Streaming and downloads active",
                        onClick = { viewModel.toggleOfflineModeOnly() }
                    )
                }
            }
        }

        // 4. Data, Backup & Share Section
        item {
            Text(
                text = "Data & Backup",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ObsidianBlack,
                letterSpacing = (-0.2).sp
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SoftSurfaceGray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRowItem(
                        icon = Icons.Filled.Share,
                        title = "Backup & Import (JSON)",
                        subtitle = "Export, share, or import playlists, favorites, & settings",
                        onClick = { viewModel.showBackupRestoreDialog(true) }
                    )
                }
            }
        }

        // 5. Notifications & General
        item {
            Text(
                text = "General",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ObsidianBlack,
                letterSpacing = (-0.2).sp
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SoftSurfaceGray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRowItem(
                        icon = Icons.Filled.Notifications,
                        title = "Notifications",
                        subtitle = "Activity feed & new releases",
                        onClick = { viewModel.showNotificationsDialog(true) }
                    )
                    HorizontalDivider(color = LightBorderGray.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsRowItem(
                        icon = Icons.Filled.Lock,
                        title = "Privacy & Security",
                        subtitle = "Local encryption enabled",
                        onClick = { }
                    )
                    HorizontalDivider(color = LightBorderGray.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsRowItem(
                        icon = Icons.Filled.HelpOutline,
                        title = "Help & Support",
                        subtitle = "FAQs and Contact Support",
                        onClick = { }
                    )
                    HorizontalDivider(color = LightBorderGray.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsRowItem(
                        icon = Icons.Filled.Info,
                        title = "About",
                        subtitle = "Vibe Music v2.4.0 (Build 2026)",
                        onClick = { }
                    )
                }
            }
        }
    }

    if (uiState.showBackupRestoreDialog) {
        BackupRestoreDialog(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.showBackupRestoreDialog(false) }
        )
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CanvasBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ObsidianBlack,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MediumGray
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MediumGray,
            modifier = Modifier.size(18.dp)
        )
    }
}
