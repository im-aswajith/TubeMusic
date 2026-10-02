package com.example.data.model

enum class SearchFilterType(val displayName: String) {
    ALL("All"),
    LIVE("🔴 Live Now"),
    VIDEOS("Videos"),
    SHORTS("Shorts")
}

enum class SortOrder(val displayName: String) {
    RELEVANCE("Relevance"),
    DATE("Upload Date"),
    VIEW_COUNT("View Count")
}
