package com.sentinelrss

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sentinelrss.data.local.AppDatabase
import com.sentinelrss.domain.FeedUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SentinelApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("SentinelApp", "App started")

        try {
            System.loadLibrary("sqlcipher")
        } catch (e: Exception) {
            Log.e("SentinelApp", "Failed to load sqlcipher", e)
        }

        ensureDatabaseSeeded()
        scheduleFeedUpdates()
    }

    private fun ensureDatabaseSeeded() {
        val db = AppDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            if (db.feedDao().getAllFeedsSync().isEmpty()) {
                Log.d("SentinelApp", "Seeding database...")
                AppDatabase.populateDatabase(db.feedDao())
            }
        }
    }

    private fun scheduleFeedUpdates() {
        val workManager = WorkManager.getInstance(this)

        // 1. Trigger immediate update
        val oneTimeRequest = OneTimeWorkRequestBuilder<FeedUpdateWorker>().build()
        workManager.enqueueUniqueWork(
            "FeedUpdateInitial",
            ExistingWorkPolicy.KEEP,
            oneTimeRequest
        )

        // 2. Schedule periodic feed updates every 15 minutes
        val periodicRequest = PeriodicWorkRequestBuilder<FeedUpdateWorker>(
            15, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "FeedUpdate",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }
}
