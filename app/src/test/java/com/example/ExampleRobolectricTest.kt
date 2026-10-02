package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalPreferencesRepository
import com.example.data.model.YouTubeVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TubeMusic", appName)
  }

  @Test
  fun `test playlist track persistence and thumbnail`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = LocalPreferencesRepository(context)

    // Verify initial empty playlist state
    val playlists = repo.getPlaylists()
    val initialPl = playlists.first()
    assertTrue("Initial tracks must be empty until manually added", initialPl.tracks.isEmpty())

    // Add track to playlist
    val track = YouTubeVideo(
      id = "song_abc_123",
      title = "Midnight Lofi Beats",
      channelTitle = "Chill Beats",
      thumbnailUrl = "https://example.com/song_thumb.jpg"
    )
    repo.addTrackToPlaylist(initialPl.id, track)

    // Verify persistence and thumbnail
    val updatedPls = repo.getPlaylists()
    val updatedPl = updatedPls.find { it.id == initialPl.id }!!
    assertEquals(1, updatedPl.tracks.size)
    assertEquals("song_abc_123", updatedPl.tracks.first().id)
    assertEquals("https://example.com/song_thumb.jpg", updatedPl.tracks.first().thumbnailUrl)
    assertEquals("https://example.com/song_thumb.jpg", updatedPl.coverUrl)
  }

  @Test
  fun `test local repository search history and bookmarks`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = LocalPreferencesRepository(context)

    // Test search history
    repo.addSearchQuery("lofi hip hop live")
    val history = repo.getSearchHistory()
    assertTrue(history.contains("lofi hip hop live"))

    // Test bookmarking video
    val testVideo = YouTubeVideo(
      id = "test_vid_123",
      title = "Test Live Stream",
      channelTitle = "Test Channel",
      thumbnailUrl = "https://example.com/thumb.jpg",
      isLive = true
    )
    val saved = repo.toggleSavedVideo(testVideo)
    assertTrue(saved)
    assertTrue(repo.isVideoSaved("test_vid_123"))

    // Toggle unsave
    val unsaved = repo.toggleSavedVideo(testVideo)
    assertEquals(false, unsaved)
    assertEquals(false, repo.isVideoSaved("test_vid_123"))
  }

  @Test
  fun `test downloaded songs persistence and management`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = LocalPreferencesRepository(context)
    val downloadManager = com.example.data.local.MusicDownloadManager(context, repo)

    val testVideo = YouTubeVideo(
      id = "offline_track_001",
      title = "Starboy High Quality",
      channelTitle = "The Weeknd",
      thumbnailUrl = "https://example.com/starboy.jpg"
    )

    val downloadedTrack = com.example.data.model.DownloadedTrack(
      video = testVideo,
      localFilePath = "${context.filesDir}/music_downloads/offline_track_001.m4a",
      fileSize = 10485760L, // 10 MB
      audioQuality = "High (320 kbps)"
    )

    // Save download
    repo.saveDownloadedTrack(downloadedTrack)
    assertTrue(repo.isDownloaded("offline_track_001"))
    val downloads = repo.getDownloadedTracks()
    assertEquals(1, downloads.size)
    assertEquals("10.0 MB", downloads.first().formattedSize)

    // Delete download
    repo.removeDownloadedTrack("offline_track_001")
    assertEquals(false, repo.isDownloaded("offline_track_001"))
    assertTrue(repo.getDownloadedTracks().isEmpty())
  }
}

