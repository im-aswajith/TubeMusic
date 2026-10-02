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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.YouTubeVideo
import com.example.ui.components.MusicTrackRow
import com.example.ui.theme.ActivePillBg
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.MediumGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurfaceGray
import com.example.ui.viewmodel.AlbumDetail
import com.example.ui.viewmodel.ArtistProfile
import com.example.ui.viewmodel.MusicPlayerUiState
import com.example.ui.viewmodel.VideoSearchViewModel

/**
 * Screen 2 & 3: Search / Search Results matching designv2-example.png
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicExploreScreen(
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val searchCategories = listOf("Songs", "Artists", "Albums", "Playlists")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
            .testTag("music_explore_screen")
    ) {
        // 1. Top Search Bar with Keyboard Search Support
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = {
                    Text(
                        "Search songs, artists, albums...",
                        color = MediumGray,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = MediumGray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(25.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.onSearchSubmitted(uiState.query)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ObsidianBlack,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = SoftSurfaceGray,
                    unfocusedContainerColor = SoftSurfaceGray,
                    focusedTextColor = ObsidianBlack,
                    unfocusedTextColor = ObsidianBlack
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("music_search_input")
            )
        }

        // 2. Category Filter Pills: Songs, Artists, Albums, Playlists
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searchCategories) { cat ->
                val isSelected = uiState.activeSearchCategory == cat
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) ObsidianBlack else SoftSurfaceGray,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { viewModel.onSearchCategoryChanged(cat) }
                        .testTag("search_cat_$cat")
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) PureWhite else ObsidianBlack,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Search Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ObsidianBlack, strokeWidth = 2.5.dp)
                }
            } else if (uiState.searchResults.isEmpty() && uiState.query.isEmpty()) {
                // Pre-Search State (Screen 2): Recent Searches & Trending Searches
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Recent Searches Section
                    if (uiState.searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianBlack
                                )
                                TextButton(onClick = { viewModel.clearAllHistory() }) {
                                    Text("Clear", color = MediumGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        items(uiState.searchHistory) { historyQuery ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.onSearchSubmitted(historyQuery)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = null,
                                        tint = MediumGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = historyQuery,
                                        fontSize = 15.sp,
                                        color = ObsidianBlack,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MediumGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Trending Searches Section
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Trending Searches",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                    }

                    items(viewModel.trendingSearches) { queryItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.onSearchSubmitted(queryItem)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = ObsidianBlack,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = queryItem,
                                    fontSize = 15.sp,
                                    color = ObsidianBlack,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MediumGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else if (uiState.searchResults.isNotEmpty()) {
                // Search Results State (Screen 3): Songs, Artists, Albums
                val firstTrack = uiState.searchResults.firstOrNull()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Songs Section
                    item {
                        Text(
                            text = "Songs",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack,
                            letterSpacing = (-0.3).sp
                        )
                    }

                    itemsIndexed(uiState.searchResults.take(6)) { index, track ->
                        MusicTrackRow(
                            track = track,
                            index = index,
                            isCurrentPlaying = uiState.currentTrack?.id == track.id,
                            isPlaying = uiState.isPlaying,
                            isSaved = viewModel.isVideoSaved(track.id),
                            onTrackClick = { viewModel.playTrack(track, uiState.searchResults) },
                            onPlayNext = { viewModel.playNextInQueue(track) },
                            onAddToQueue = { viewModel.addToQueue(track) },
                            onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                            onToggleSave = { viewModel.toggleSaveVideo(track) }
                        )
                    }

                    // 2. Artists Section
                    if (firstTrack != null) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Artists",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack,
                                letterSpacing = (-0.3).sp
                            )
                        }

                        item {
                            val artistName = firstTrack.displayArtist
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SoftSurfaceGray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.openArtistDetail(
                                            ArtistProfile(
                                                name = artistName,
                                                imageUrl = firstTrack.thumbnailUrl,
                                                followersText = "2.4M followers • 12 following"
                                            )
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE5E7EB))
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(firstTrack.thumbnailUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = artistName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column {
                                            Text(
                                                text = artistName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ObsidianBlack
                                            )
                                            Text(
                                                text = "2.4M followers",
                                                fontSize = 12.sp,
                                                color = MediumGray
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MediumGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Albums Section
                    if (firstTrack != null) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Albums",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack,
                                letterSpacing = (-0.3).sp
                            )
                        }

                        item {
                            val albumTitle = firstTrack.title.take(24)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SoftSurfaceGray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.openAlbumDetail(
                                            AlbumDetail(
                                                title = albumTitle,
                                                artist = firstTrack.displayArtist,
                                                coverUrl = firstTrack.thumbnailUrl,
                                                tracks = uiState.searchResults.take(10)
                                            )
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFE5E7EB))
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(firstTrack.thumbnailUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = albumTitle,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column {
                                            Text(
                                                text = albumTitle,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ObsidianBlack,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${firstTrack.displayArtist} • 2024",
                                                fontSize = 12.sp,
                                                color = MediumGray
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MediumGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found. Try another search query.", color = MediumGray)
                }
            }
        }
    }
}
