package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.MusicApp
import com.example.R
import com.example.data.model.YouTubeVideo
import com.example.ui.components.GlobalMusicPlayerHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var mediaSession: MediaSession? = null
    private var cachedBitmap: Bitmap? = null
    private var currentPosMs: Long = 0L
    private var currentDurationMs: Long = 0L
    private var lastNotificationUpdateTime: Long = 0L

    companion object {
        private const val TAG = "MusicPlaybackService"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_TRACK = "com.example.service.PLAY_TRACK"
        const val ACTION_PLAY = "com.example.service.PLAY"
        const val ACTION_PAUSE = "com.example.service.PAUSE"
        const val ACTION_TOGGLE_PLAY = "com.example.service.TOGGLE_PLAY"
        const val ACTION_NEXT = "com.example.service.NEXT"
        const val ACTION_PREVIOUS = "com.example.service.PREVIOUS"
        const val ACTION_STOP = "com.example.service.STOP"
        const val ACTION_UPDATE_PROGRESS = "com.example.service.UPDATE_PROGRESS"

        private val _serviceState = MutableStateFlow(ServicePlaybackState())
        val serviceState: StateFlow<ServicePlaybackState> = _serviceState.asStateFlow()

        // Callbacks from UI or background player controller
        var onNextCallback: (() -> Unit)? = null
        var onPrevCallback: (() -> Unit)? = null
        var onTogglePlayCallback: (() -> Unit)? = null
        var onStopCallback: (() -> Unit)? = null
        var onSeekCallback: ((Long) -> Unit)? = null

        fun startServiceForTrack(context: Context, video: YouTubeVideo, isPlaying: Boolean = true, durationMs: Long = 0L) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_PLAY_TRACK
                putExtra("video_id", video.id)
                putExtra("video_title", video.title)
                putExtra("channel_title", video.channelTitle)
                putExtra("thumbnail_url", video.thumbnailUrl)
                putExtra("is_playing", isPlaying)
                putExtra("dur_ms", if (durationMs > 0) durationMs else video.durationSeconds * 1000L)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun updateProgress(context: Context, posMs: Long, durationMs: Long) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_UPDATE_PROGRESS
                putExtra("pos_ms", posMs)
                putExtra("dur_ms", durationMs)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun updatePlaybackStatus(context: Context, isPlaying: Boolean) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = if (isPlaying) ACTION_PLAY else ACTION_PAUSE
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    private val noisyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                GlobalMusicPlayerHolder.pauseDirectly()
                _serviceState.value = _serviceState.value.copy(isPlaying = false)
                val current = _serviceState.value
                updateMediaSessionState(false, current.title, current.artist, cachedBitmap, currentPosMs, currentDurationMs)
                showForegroundNotification(current.title, current.artist, current.thumbnailUrl, false, currentPosMs, currentDurationMs)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TubeMusic:PlaybackWakeLock")

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "TubeMusic:WifiLock")

        try {
            val filter = android.content.IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            registerReceiver(noisyReceiver, filter)
        } catch (_: Exception) {}

        initMediaSession()
    }

    private fun initMediaSession() {
        try {
            mediaSession = MediaSession(this, "SpotifyMediaSession").apply {
                setFlags(
                    MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
                )
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        if (onTogglePlayCallback != null) {
                            onTogglePlayCallback?.invoke()
                        } else {
                            GlobalMusicPlayerHolder.playDirectly()
                            _serviceState.value = _serviceState.value.copy(isPlaying = true)
                            val current = _serviceState.value
                            updateMediaSessionState(true, current.title, current.artist)
                            showForegroundNotification(current.title, current.artist, current.thumbnailUrl, true)
                        }
                    }

                    override fun onPause() {
                        if (onTogglePlayCallback != null) {
                            onTogglePlayCallback?.invoke()
                        } else {
                            GlobalMusicPlayerHolder.pauseDirectly()
                            _serviceState.value = _serviceState.value.copy(isPlaying = false)
                            val current = _serviceState.value
                            updateMediaSessionState(false, current.title, current.artist)
                            showForegroundNotification(current.title, current.artist, current.thumbnailUrl, false)
                        }
                    }

                    override fun onSkipToNext() {
                        onNextCallback?.invoke()
                    }

                    override fun onSkipToPrevious() {
                        onPrevCallback?.invoke()
                    }

                    override fun onSeekTo(pos: Long) {
                        currentPosMs = pos
                        GlobalMusicPlayerHolder.seekToDirectly(pos / 1000f)
                        onSeekCallback?.invoke(pos)
                        val cur = _serviceState.value
                        if (cur.isActive) {
                            updateMediaSessionState(cur.isPlaying, cur.title, cur.artist, cachedBitmap, pos, currentDurationMs)
                            val notification = buildNotification(cur.title, cur.artist, cachedBitmap, cur.isPlaying, pos, currentDurationMs)
                            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                            manager?.notify(NOTIFICATION_ID, notification)
                        }
                    }

                    override fun onStop() {
                        GlobalMusicPlayerHolder.pauseDirectly()
                        onStopCallback?.invoke()
                        stopForeground(true)
                        stopSelf()
                    }
                })
                isActive = true
            }
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY

        when (action) {
            ACTION_PLAY_TRACK -> {
                val videoId = intent.getStringExtra("video_id") ?: ""
                val title = intent.getStringExtra("video_title") ?: "Music Track"
                val artist = intent.getStringExtra("channel_title") ?: "Artist"
                val thumb = intent.getStringExtra("thumbnail_url") ?: ""
                val isPlaying = intent.getBooleanExtra("is_playing", true)
                val dur = intent.getLongExtra("dur_ms", 0L)
                currentPosMs = 0L
                if (dur > 0) currentDurationMs = dur

                _serviceState.value = ServicePlaybackState(
                    currentVideoId = videoId,
                    title = title,
                    artist = artist,
                    thumbnailUrl = thumb,
                    isPlaying = isPlaying,
                    isActive = true,
                    posMs = 0L,
                    durationMs = currentDurationMs
                )
                acquireWakeLock()
                showForegroundNotification(title, artist, thumb, isPlaying, 0L, currentDurationMs)
            }
            ACTION_PLAY -> {
                _serviceState.value = _serviceState.value.copy(isPlaying = true)
                acquireWakeLock()
                val current = _serviceState.value
                GlobalMusicPlayerHolder.playDirectly()
                updateMediaSessionState(true, current.title, current.artist, cachedBitmap, currentPosMs, currentDurationMs)
                showForegroundNotification(current.title, current.artist, current.thumbnailUrl, true, currentPosMs, currentDurationMs)
            }
            ACTION_PAUSE -> {
                _serviceState.value = _serviceState.value.copy(isPlaying = false)
                releaseWakeLock()
                val current = _serviceState.value
                GlobalMusicPlayerHolder.pauseDirectly()
                updateMediaSessionState(false, current.title, current.artist, cachedBitmap, currentPosMs, currentDurationMs)
                showForegroundNotification(current.title, current.artist, current.thumbnailUrl, false, currentPosMs, currentDurationMs)
            }
            ACTION_TOGGLE_PLAY -> {
                if (onTogglePlayCallback != null) {
                    onTogglePlayCallback?.invoke()
                } else {
                    GlobalMusicPlayerHolder.togglePlayDirectly()
                    val newPlaying = !_serviceState.value.isPlaying
                    _serviceState.value = _serviceState.value.copy(isPlaying = newPlaying)
                    val current = _serviceState.value
                    updateMediaSessionState(newPlaying, current.title, current.artist, cachedBitmap, currentPosMs, currentDurationMs)
                    showForegroundNotification(current.title, current.artist, current.thumbnailUrl, newPlaying, currentPosMs, currentDurationMs)
                }
            }
            ACTION_NEXT -> {
                onNextCallback?.invoke()
            }
            ACTION_PREVIOUS -> {
                onPrevCallback?.invoke()
            }
            ACTION_UPDATE_PROGRESS -> {
                val pos = intent.getLongExtra("pos_ms", 0L)
                val dur = intent.getLongExtra("dur_ms", 0L)
                currentPosMs = pos
                if (dur > 0) currentDurationMs = dur
                val cur = _serviceState.value
                if (cur.isActive) {
                    _serviceState.value = cur.copy(posMs = pos, durationMs = currentDurationMs)
                    updateMediaSessionState(cur.isPlaying, cur.title, cur.artist, cachedBitmap, pos, currentDurationMs)
                    val now = System.currentTimeMillis()
                    if (now - lastNotificationUpdateTime >= 1000L) {
                        lastNotificationUpdateTime = now
                        val notification = buildNotification(cur.title, cur.artist, cachedBitmap, cur.isPlaying, pos, currentDurationMs)
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                        manager?.notify(NOTIFICATION_ID, notification)
                    }
                }
            }
            ACTION_STOP -> {
                _serviceState.value = ServicePlaybackState()
                releaseWakeLock()
                GlobalMusicPlayerHolder.pauseDirectly()
                onStopCallback?.invoke()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val current = _serviceState.value
        // If track is active, continue running foreground service in background even if app is closed/swiped from recents
        if (current.isActive) {
            acquireWakeLock()
            return
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun updateMediaSessionState(
        isPlaying: Boolean,
        title: String,
        artist: String,
        albumArt: Bitmap? = null,
        posMs: Long = 0L,
        durationMs: Long = 0L
    ) {
        try {
            val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val actions = PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_STOP

            val playbackPosition = if (posMs > 0) posMs else PlaybackState.PLAYBACK_POSITION_UNKNOWN

            mediaSession?.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(actions)
                    .setState(state, playbackPosition, 1.0f)
                    .build()
            )

            val metaBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, "Spotify Lounge")
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, artist)

            if (durationMs > 0) {
                metaBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)
            }
            if (albumArt != null) {
                metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt)
                metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, albumArt)
            }

            mediaSession?.setMetadata(metaBuilder.build())
        } catch (_: Exception) {}
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours timeout
            }
            if (wifiLock?.isHeld != true) {
                wifiLock?.acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {}
    }

    private fun showForegroundNotification(
        title: String,
        artist: String,
        thumbnailUrl: String,
        isPlaying: Boolean,
        posMs: Long = currentPosMs,
        durationMs: Long = currentDurationMs
    ) {
        // 1. Immediately post foreground notification synchronously to satisfy Android's 5-second requirement and prevent ANR/Force Stop!
        updateMediaSessionState(isPlaying, title, artist, cachedBitmap, posMs, durationMs)
        val initialNotification = buildNotification(title, artist, cachedBitmap, isPlaying, posMs, durationMs)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
        }

        // 2. Asynchronously load high-res thumbnail and update notification smoothly
        if (thumbnailUrl.isNotBlank()) {
            serviceScope.launch {
                val bitmap = loadThumbnailBitmap(thumbnailUrl)
                if (bitmap != null) {
                    cachedBitmap = bitmap
                    updateMediaSessionState(isPlaying, title, artist, bitmap, posMs, durationMs)
                    val updatedNotification = buildNotification(title, artist, bitmap, isPlaying, posMs, durationMs)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    notificationManager?.notify(NOTIFICATION_ID, updatedNotification)
                }
            }
        }
    }

    private suspend fun loadThumbnailBitmap(url: String): Bitmap? {
        if (url.isBlank()) return null
        return try {
            val loader = ImageLoader(this)
            val request = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false)
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            (result as? BitmapDrawable)?.bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun buildNotification(
        title: String,
        artist: String,
        largeIcon: Bitmap?,
        isPlaying: Boolean,
        posMs: Long = currentPosMs,
        durationMs: Long = currentDurationMs
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_TOGGLE_PLAY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        // Spotify-style Native Media Controller Notification
        val mediaStyle = Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)

        mediaSession?.sessionToken?.let { token ->
            mediaStyle.setMediaSession(token)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, MusicApp.MUSIC_NOTIFICATION_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val prevAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_previous),
            "Previous",
            prevIntent
        ).build()

        val playPauseAction = Notification.Action.Builder(
            Icon.createWithResource(this, playPauseIcon),
            playPauseTitle,
            toggleIntent
        ).build()

        val nextAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_next),
            "Next",
            nextIntent
        ).build()

        builder
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("Spotify Media Controller")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setOngoing(isPlaying)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(mediaStyle)

        // Song Seek bar / Progress bar in notification
        if (durationMs > 0) {
            val totalSec = (durationMs / 1000).toInt().coerceAtLeast(1)
            val curSec = (posMs / 1000).toInt().coerceIn(0, totalSec)
            builder.setProgress(totalSec, curSec, false)
        }

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setColorized(true)
        }

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try { unregisterReceiver(noisyReceiver) } catch (_: Exception) {}
        releaseWakeLock()
        mediaSession?.release()
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }
}

data class ServicePlaybackState(
    val currentVideoId: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    val isPlaying: Boolean = false,
    val isActive: Boolean = false,
    val posMs: Long = 0L,
    val durationMs: Long = 0L
)
