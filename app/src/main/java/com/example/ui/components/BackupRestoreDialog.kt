package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ActivePillBg
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.LightBorderGray
import com.example.ui.theme.MediumGray
import com.example.ui.theme.MusicPrimary
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurfaceGray
import com.example.ui.viewmodel.MusicPlayerUiState
import com.example.ui.viewmodel.VideoSearchViewModel
import org.json.JSONObject

@Composable
fun BackupRestoreDialog(
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Export, 1: Import

    // Export Options
    var incPlaylists by remember { mutableStateOf(true) }
    var incLiked by remember { mutableStateOf(true) }
    var incSettings by remember { mutableStateOf(true) }
    var incRecents by remember { mutableStateOf(true) }

    // Export JSON preview
    var generatedJson by remember { mutableStateOf("") }
    LaunchedEffect(incPlaylists, incLiked, incSettings, incRecents, uiState.playlists, uiState.savedVideos) {
        generatedJson = viewModel.exportBackupJson(incPlaylists, incLiked, incSettings, incRecents)
    }

    // Import State
    var importText by remember { mutableStateOf("") }
    var mergeData by remember { mutableStateOf(true) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var importSuccess by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("backup_restore_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Backup & Share",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "Save, share, or restore in JSON format",
                            fontSize = 12.sp,
                            color = MediumGray
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = ObsidianBlack)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Export / Import
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SoftSurfaceGray,
                    contentColor = ObsidianBlack,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MusicPrimary,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export / Share", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import / Restore", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                if (selectedTab == 0) {
                    // EXPORT TAB
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Choose what to include:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ObsidianBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Checkboxes
                        ExportCheckboxRow(
                            title = "Playlists",
                            subtitle = "${uiState.playlists.size} playlists (${uiState.playlists.sumOf { it.tracks.size }} songs)",
                            checked = incPlaylists,
                            onCheckedChange = { incPlaylists = it }
                        )
                        ExportCheckboxRow(
                            title = "Liked Songs (Favorites)",
                            subtitle = "${uiState.savedVideos.size} tracks saved",
                            checked = incLiked,
                            onCheckedChange = { incLiked = it }
                        )
                        ExportCheckboxRow(
                            title = "Audio & App Settings",
                            subtitle = "${uiState.streamingQuality} streaming, EQ preset, ${uiState.selectedTheme} theme",
                            checked = incSettings,
                            onCheckedChange = { incSettings = it }
                        )
                        ExportCheckboxRow(
                            title = "Recently Played History",
                            subtitle = "${uiState.recentlyPlayed.size} tracks in history",
                            checked = incRecents,
                            onCheckedChange = { incRecents = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // JSON Preview Box
                        Text(
                            text = "JSON Payload Preview (${generatedJson.length} bytes):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MediumGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftSurfaceGray)
                                .border(1.dp, LightBorderGray, RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = generatedJson.take(600) + if (generatedJson.length > 600) "\n... [truncated]" else "",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ObsidianBlack
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions: Share & Copy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("VibeMusic_Backup", generatedJson)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied JSON to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftSurfaceGray, contentColor = ObsidianBlack),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy JSON", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    viewModel.shareBackupJson(context, generatedJson)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MusicPrimary, contentColor = PureWhite),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share JSON", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // IMPORT TAB
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Paste JSON backup or shared playlist code:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ObsidianBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = importText,
                            onValueChange = {
                                importText = it
                                importStatus = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = {
                                Text("{\n  \"app\": \"VibeMusic\",\n  \"playlists\": [...]\n}", fontSize = 11.sp, color = MediumGray)
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MusicPrimary,
                                unfocusedBorderColor = LightBorderGray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Paste from Clipboard",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MusicPrimary,
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        importText = text
                                    } else {
                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Merge vs Replace
                        Text(
                            text = "Import Strategy:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mergeData,
                                onClick = { mergeData = true },
                                colors = RadioButtonDefaults.colors(selectedColor = MusicPrimary)
                            )
                            Text(
                                text = "Merge with existing data (Recommended)",
                                fontSize = 12.sp,
                                color = ObsidianBlack,
                                modifier = Modifier.clickable { mergeData = true }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !mergeData,
                                onClick = { mergeData = false },
                                colors = RadioButtonDefaults.colors(selectedColor = MusicPrimary)
                            )
                            Text(
                                text = "Replace existing data",
                                fontSize = 12.sp,
                                color = ObsidianBlack,
                                modifier = Modifier.clickable { mergeData = false }
                            )
                        }

                        // Feedback Status
                        if (importStatus != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (importSuccess) Color(0xFF1DB954).copy(alpha = 0.15f) else Color(0xFFFF2D55).copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = importStatus!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (importSuccess) Color(0xFF1DB954) else Color(0xFFFF2D55),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (importText.isBlank()) {
                                    importStatus = "Please enter or paste valid JSON code."
                                    importSuccess = false
                                    return@Button
                                }
                                val result = viewModel.importBackupJson(importText, mergeData)
                                if (result.errorMessage != null) {
                                    importStatus = "Error: ${result.errorMessage}"
                                    importSuccess = false
                                } else {
                                    importStatus = "Success! Restored ${result.playlistsCount} playlists, ${result.likedCount} liked songs, and settings."
                                    importSuccess = true
                                    Toast.makeText(context, "Import successful!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MusicPrimary, contentColor = PureWhite),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore / Import Now", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportCheckboxRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = MusicPrimary)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ObsidianBlack)
            Text(text = subtitle, fontSize = 11.sp, color = MediumGray)
        }
    }
}
