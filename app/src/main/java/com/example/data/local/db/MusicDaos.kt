package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks")
    fun getAllTracksFlow(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR channelTitle LIKE '%' || :query || '%'")
    fun searchTracksFlow(query: String): Flow<List<TrackEntity>>

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)
}

@Dao
interface LikedSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiked(liked: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE videoId = :videoId")
    suspend fun deleteLiked(videoId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE videoId = :videoId)")
    suspend fun isLiked(videoId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE videoId = :videoId)")
    fun isLikedFlow(videoId: String): Flow<Boolean>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN liked_songs l ON t.id = l.videoId
        ORDER BY l.likedAt DESC
    """)
    fun getLikedTracksFlow(): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN liked_songs l ON t.id = l.videoId
        ORDER BY l.likedAt DESC
    """)
    suspend fun getLikedTracks(): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM liked_songs")
    fun getLikedCountFlow(): Flow<Int>
}

@Dao
interface RecentlyPlayedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentlyPlayedEntity)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN recently_played r ON t.id = r.videoId
        ORDER BY r.playedAt DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayedTracksFlow(limit: Int = 50): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN recently_played r ON t.id = r.videoId
        ORDER BY r.playedAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentlyPlayedTracks(limit: Int = 50): List<TrackEntity>

    @Query("DELETE FROM recently_played WHERE videoId = :videoId")
    suspend fun deleteRecent(videoId: String)

    @Query("DELETE FROM recently_played")
    suspend fun clearAll()
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(ref: PlaylistTrackCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(refs: List<PlaylistTrackCrossRef>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeTrackFromPlaylist(playlistId: String, videoId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearTracksForPlaylist(playlistId: String)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.videoId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.sortOrder ASC, pt.addedAt ASC
    """)
    fun getTracksForPlaylistFlow(playlistId: String): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.videoId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.sortOrder ASC, pt.addedAt ASC
    """)
    suspend fun getTracksForPlaylist(playlistId: String): List<TrackEntity>

    @Query("SELECT videoId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY sortOrder ASC, addedAt ASC")
    suspend fun getVideoIdsForPlaylist(playlistId: String): List<String>
}

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE videoId = :videoId LIMIT 1")
    suspend fun getDownloadById(videoId: String): DownloadEntity?

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun deleteDownload(videoId: String)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN downloads d ON t.id = d.videoId
        ORDER BY d.downloadedAt DESC
    """)
    suspend fun getAllDownloadsRaw(): List<TrackEntity>

    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAllDownloadsFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    suspend fun getAllDownloads(): List<DownloadEntity>

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}

@Dao
interface ListeningStatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordListeningEvent(event: ListeningStatEntity)

    @Query("SELECT SUM(durationSeconds) FROM listening_stats")
    fun getTotalListeningSecondsFlow(): Flow<Long?>

    @Query("SELECT SUM(durationSeconds) FROM listening_stats WHERE timestamp >= :sinceTimestamp")
    fun getListeningSecondsSinceFlow(sinceTimestamp: Long): Flow<Long?>

    @Query("""
        SELECT videoId, title, artist, COUNT(*) as playCount, SUM(durationSeconds) as totalTimeSec
        FROM listening_stats
        GROUP BY videoId
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    fun getMostPlayedFlow(limit: Int = 10): Flow<List<TopTrackStat>>

    @Query("""
        SELECT artist, COUNT(*) as playCount, SUM(durationSeconds) as totalTimeSec
        FROM listening_stats
        GROUP BY artist
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    fun getTopArtistsFlow(limit: Int = 10): Flow<List<TopArtistStat>>

    @Query("SELECT COUNT(*) FROM listening_stats")
    fun getTotalPlaysFlow(): Flow<Int>
}

data class TopTrackStat(
    val videoId: String,
    val title: String,
    val artist: String,
    val playCount: Int,
    val totalTimeSec: Long
)

data class TopArtistStat(
    val artist: String,
    val playCount: Int,
    val totalTimeSec: Long
)
