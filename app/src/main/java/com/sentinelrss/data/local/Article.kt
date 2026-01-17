package com.sentinelrss.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "articles", indices = [Index(value = ["link"], unique = true)])
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
