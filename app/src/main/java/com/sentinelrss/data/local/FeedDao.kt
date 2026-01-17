package com.sentinelrss.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM feeds")
    fun getAllFeeds(): Flow<List<Feed>>

    @Query("SELECT * FROM feeds")
    suspend fun getAllFeedsSync(): List<Feed>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: Feed)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFeeds(feeds: List<Feed>)

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteFeed(id: Long)
}
