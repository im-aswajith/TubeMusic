package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MusicApp : Application() {
    companion object {
        const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_playback_channel"
        lateinit var instance: MusicApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TubeMusic Playback"
            val descriptionText = "Controls and status for currently playing music and background streams"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(MUSIC_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
