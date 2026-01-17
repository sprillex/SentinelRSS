package com.sentinelrss

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sentinelrss.domain.FeedUpdateWorker
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

        scheduleFeedUpdates()
    }

    private fun scheduleFeedUpdates() {
        // Schedule periodic feed updates every 15 minutes (minimum allowed by Android)
        val workRequest = PeriodicWorkRequestBuilder<FeedUpdateWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FeedUpdate",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
