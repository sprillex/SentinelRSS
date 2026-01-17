package com.sentinelrss.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE isCulled = 0 ORDER BY score DESC, pubDate DESC")
    fun getRelevantArticles(): Flow<List<Article>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticle(article: Article): Long

    @Query("UPDATE articles SET isCulled = 1 WHERE id = :id")
    suspend fun cullArticle(id: Long)

    @Query("SELECT * FROM articles WHERE feedId = :feedId")
    suspend fun getArticlesForFeed(feedId: Long): List<Article>

    @Query("SELECT * FROM articles WHERE isRead = 0 AND isCulled = 0")
    suspend fun getUnreadArticles(): List<Article>

    @Query("UPDATE articles SET vectorEmbedding = :embedding, score = :score WHERE id = :id")
    suspend fun updateScore(id: Long, embedding: String, score: Float)
}
