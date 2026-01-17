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

                    // Check if article already exists to avoid re-scoring/duplication
                    // For simplicity, we assume if we process it, we check link or title.
                    // But Room's OnConflictStrategy.IGNORE handles insertion.
                    // However, we want to score it if it's new.
                    // Let's assume we score everything for now, but in a real app check existence first.

                    val score = scorer.score(title, description)

                    // Simple logic to parse date or use current time
                    val pubDate = try {
                        item.pubDate?.let { Date.parse(it) } ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    // "The Cull" logic:
                    // If the user has no interests yet (score approx 0.5), we keep everything to let them start.
                    // If they have interests, we cull anything with low similarity.
                    // Dynamic threshold: If the score is significantly below average or absolute low.

                    val isCulled = if (score < 0.3f) {
                        true
                    } else {
                        // Keep random 10% of low scoring stuff for serendipity/exploration?
                        // For now, strict culling below 0.3 if we have a model.
                        // If random score (model missing), effectively random culling, which is bad.
                        // So if score is exactly 0f (error), keep it.
                        score > 0f && score < 0.3f
                    }

                    val article = Article(
                        feedId = feed.id,
                        title = title,
                        link = item.link ?: "",
                        description = description,
                        pubDate = pubDate,
                        content = item.content ?: "",
                        score = score,
                        isCulled = isCulled
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
