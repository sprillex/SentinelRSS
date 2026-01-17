package com.sentinelrss.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sentinelrss.data.local.AppDatabase
import com.sentinelrss.data.local.Article
import com.sentinelrss.data.remote.RssFetcher
import java.util.Date

class FeedUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = AppDatabase.getDatabase(context)
    private val rssFetcher = RssFetcher()
    private val scorer = ContentScorer(context)

    override suspend fun doWork(): Result {
        val feeds = database.feedDao().getAllFeedsSync()

        for (feed in feeds) {
            try {
                val items = rssFetcher.fetchFeed(feed.url)
                val limitedItems = items.take(feed.downloadLimit)

                for (item in limitedItems) {
                    val title = item.title ?: "No Title"
                    val description = item.description ?: ""

                    val score = scorer.score(title, description)

                    // Simple logic to parse date or use current time
                    val pubDate = try {
                        item.pubDate?.let { Date.parse(it) } ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    val article = Article(
                        feedId = feed.id,
                        title = title,
                        link = item.link ?: "",
                        description = description,
                        pubDate = pubDate,
                        content = item.content ?: "",
                        score = score,
                        isCulled = score < 0.3f // "The Cull" threshold
                    )
                    database.articleDao().insertArticle(article)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success()
    }
}
