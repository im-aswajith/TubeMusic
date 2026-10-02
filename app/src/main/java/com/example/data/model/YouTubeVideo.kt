package com.example.data.model

data class YouTubeVideo(
    val id: String,
    val title: String,
    val channelTitle: String,
    val channelThumbnailUrl: String? = null,
    val thumbnailUrl: String,
    val duration: String? = null,
    val viewCountText: String? = null,
    val publishedTimeText: String? = null,
    val isLive: Boolean = false,
    val description: String? = null,
    val watchUrl: String = "https://www.youtube.com/watch?v=$id"
) {
    val durationSeconds: Long
        get() {
            if (duration.isNullOrBlank() || isLive) return 0L
            val parts = duration.split(":").mapNotNull { it.trim().toLongOrNull() }
            return when (parts.size) {
                1 -> parts[0]
                2 -> parts[0] * 60 + parts[1]
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                else -> 0L
            }
        }

    val maxResThumbnailUrl: String
        get() = "https://i.ytimg.com/vi/$id/maxresdefault.jpg"

    val displayArtist: String
        get() {
            val cleanChannel = channelTitle
                .replace(" - Topic", "")
                .replace("VEVO", "")
                .replace("Official", "")
                .trim()
            if (cleanChannel.isNotBlank() && cleanChannel != "YouTube" && cleanChannel != "Music") {
                return cleanChannel
            }
            if (title.contains(" - ")) {
                val candidate = title.substringBefore(" - ").trim()
                if (candidate.length in 2..40) return candidate
            }
            return if (cleanChannel.isNotBlank()) cleanChannel else "Artist"
        }
}
