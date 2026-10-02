package com.example.data.local.db

import android.content.Context
import android.util.Log
import com.example.data.local.LocalPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseMigrationHelper {
    private const val TAG = "DbMigrationHelper"
    private const val PREF_MIGRATION_DONE = "room_migration_v1_completed"

    suspend fun migrateLegacyPreferencesIfNeeded(
        context: Context,
        database: AppDatabase,
        legacyRepo: LocalPreferencesRepository
    ) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("tubemusic_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_MIGRATION_DONE, false)) {
            return@withContext
        }

        try {
            Log.i(TAG, "Starting legacy data migration into Room database...")
            val trackDao = database.trackDao()
            val likedDao = database.likedSongDao()
            val recentDao = database.recentlyPlayedDao()
            val playlistDao = database.playlistDao()
            val downloadDao = database.downloadDao()

            // 1. Liked songs
            val likedVideos = legacyRepo.getSavedVideos()
            likedVideos.forEach { video ->
                trackDao.insertTrack(TrackEntity.fromVideo(video))
                likedDao.insertLiked(LikedSongEntity(videoId = video.id))
            }

            // 2. Recently played
            val recentVideos = legacyRepo.getRecentlyPlayed()
            recentVideos.forEachIndexed { index, video ->
                trackDao.insertTrack(TrackEntity.fromVideo(video))
                val timeOffset = System.currentTimeMillis() - (index * 60_000L)
                recentDao.insertRecent(RecentlyPlayedEntity(videoId = video.id, playedAt = timeOffset))
            }

            // 3. Playlists & Playlist Tracks
            val playlists = legacyRepo.getPlaylists()
            playlists.forEach { pl ->
                val entity = PlaylistEntity(
                    id = pl.id,
                    title = pl.title,
                    description = pl.description,
                    coverUrl = pl.coverUrl,
                    createdAt = pl.createdAt,
                    updatedAt = pl.createdAt
                )
                playlistDao.insertPlaylist(entity)

                pl.tracks.forEachIndexed { idx, track ->
                    trackDao.insertTrack(TrackEntity.fromVideo(track))
                    playlistDao.insertPlaylistTrack(
                        PlaylistTrackCrossRef(
                            playlistId = pl.id,
                            videoId = track.id,
                            sortOrder = idx,
                            addedAt = pl.createdAt + idx
                        )
                    )
                }
            }

            // 4. Downloaded tracks
            val downloadedTracks = legacyRepo.getDownloadedTracks()
            downloadedTracks.forEach { dt ->
                trackDao.insertTrack(TrackEntity.fromVideo(dt.video))
                downloadDao.insertDownload(
                    DownloadEntity(
                        videoId = dt.video.id,
                        localFilePath = dt.localFilePath,
                        fileSize = dt.fileSize,
                        downloadedAt = dt.downloadedAt,
                        audioQuality = dt.audioQuality,
                        status = "COMPLETED"
                    )
                )
            }

            prefs.edit().putBoolean(PREF_MIGRATION_DONE, true).apply()
            Log.i(TAG, "Successfully migrated ${likedVideos.size} liked songs, ${recentVideos.size} recents, ${playlists.size} playlists, ${downloadedTracks.size} downloads to Room!")
        } catch (e: Exception) {
            Log.e(TAG, "Legacy migration error", e)
        }
    }
}
