package com.sentinelrss.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val feedId: Long,
    val title: String,
    val link: String,
    val description: String,
    val pubDate: Long,
    val content: String,
    val vectorEmbedding: String? = null, // Store as string for simplicity
    val score: Float = 0f,
    val isRead: Boolean = false,
    val isCulled: Boolean = false
)
