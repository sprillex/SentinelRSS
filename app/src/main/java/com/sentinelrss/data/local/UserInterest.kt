package com.sentinelrss.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_interests")
data class UserInterest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vectorEmbedding: String, // Stored as comma-separated string or serialized float array
    val createdAt: Long = System.currentTimeMillis()
)
