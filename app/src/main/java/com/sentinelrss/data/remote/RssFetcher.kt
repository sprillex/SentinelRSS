package com.sentinelrss.data.remote

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssItem

class RssFetcher {
    private val parser = RssParser()

    suspend fun fetchFeed(url: String): List<RssItem> {
        return try {
            val channel = parser.getRssChannel(url)
            channel.items
        } catch (e: Exception) {
            e.printStackTrace()
            // In a real app, log error or propagate
            emptyList()
        }
    }
}
