package com.sentinelrss.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sentinelrss.data.local.AppDatabase
import com.sentinelrss.data.local.UserInterest
import com.sentinelrss.domain.ContentScorer
import com.sentinelrss.domain.FeedUpdateWorker
import com.sentinelrss.utils.ModelUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyTo
import java.io.File
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KtorServerService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                embeddedServer(CIO, port = 8080) {
                    install(ContentNegotiation) {
                        json()
                    }

                    val database = AppDatabase.getDatabase(applicationContext)
                    val scorer = ContentScorer(applicationContext)

                    routing {
                        get("/") {
                            call.respondText(DashboardHtml.getHtml(), ContentType.Text.Html)
                        }

                        get("/feed.xml") {
                            val token = call.request.queryParameters["token"]
                            if (token != "mysecrettoken") { // Very basic token auth as per discussion
                                call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                                return@get
                            }

                            val minRatingStr = call.request.queryParameters["minrating"]
                            val maxRatingStr = call.request.queryParameters["maxrating"]
                            val discStr = call.request.queryParameters["disc"]

                            // Map 0-10 rating to 0.0-1.0 score
                            val minScore = (minRatingStr?.toFloatOrNull() ?: 0f) / 10f
                            val maxScore = (maxRatingStr?.toFloatOrNull() ?: 10f) / 10f
                            val includeCulled = discStr?.lowercase() != "no"

                            try {
                                val articles = database.articleDao().getFeedArticles(minScore, maxScore, includeCulled)

                                val rfc822Format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }

                                val rssBuilder = StringBuilder()
                                rssBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                                rssBuilder.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n")
                                rssBuilder.append("  <channel>\n")
                                rssBuilder.append("    <title>SentinelRSS Curated Feed</title>\n")
                                rssBuilder.append("    <link>http://localhost:8080/</link>\n")
                                rssBuilder.append("    <description>Your personalized RSS feed powered by Machine Learning</description>\n")

                                for (article in articles) {
                                    rssBuilder.append("    <item>\n")
                                    rssBuilder.append("      <title><![CDATA[${article.title}]]></title>\n")
                                    rssBuilder.append("      <link><![CDATA[${article.link}]]></link>\n")
                                    rssBuilder.append("      <guid isPermaLink=\"false\">${article.id}</guid>\n")
                                    rssBuilder.append("      <description><![CDATA[${article.description}]]></description>\n")
                                    val pubDateStr = rfc822Format.format(Date(article.pubDate))
                                    rssBuilder.append("      <pubDate>$pubDateStr</pubDate>\n")
                                    rssBuilder.append("    </item>\n")
                                }

                                rssBuilder.append("  </channel>\n")
                                rssBuilder.append("</rss>\n")

                                call.respondText(rssBuilder.toString(), ContentType.Application.Rss)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                call.respondText("Error generating feed", status = HttpStatusCode.InternalServerError)
                            }
                        }

                        get("/api/articles") {
                            try {
                                val articles = database.articleDao().getRelevantArticles().first()
                                call.respond(articles)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                call.respondText("Error fetching articles", status = HttpStatusCode.InternalServerError)
                            }
                        }

                        get("/api/status") {
                            val isModelLoaded = scorer.isModelLoaded()
                            call.respond(mapOf("ml_model_loaded" to isModelLoaded))
                        }

                        post("/api/model/download") {
                            try {
                                val client = HttpClient(io.ktor.client.engine.cio.CIO)
                                val response = client.get(ModelUtils.MODEL_URL)

                                if (response.status == HttpStatusCode.OK) {
                                    val file = File(applicationContext.filesDir, ModelUtils.MODEL_FILENAME)
                                    response.bodyAsChannel().copyTo(file.writeChannel())

                                    // Reload scorer
                                    scorer.loadModel()

                                    call.respondText("Model downloaded successfully", status = HttpStatusCode.OK)
                                } else {
                                    call.respondText("Failed to download model", status = HttpStatusCode.BadGateway)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                call.respondText("Error downloading model: ${e.message}", status = HttpStatusCode.InternalServerError)
                            }
                        }

                        post("/api/refresh") {
                            val request = OneTimeWorkRequestBuilder<FeedUpdateWorker>().build()
                            WorkManager.getInstance(applicationContext).enqueue(request)
                            call.respondText("Refresh triggered", status = HttpStatusCode.OK)
                        }

                        post("/api/articles/{id}/like") {
                            val id = call.parameters["id"]?.toLongOrNull()
                            if (id != null) {
                                try {
                                    // 1. Get article
                                    // In a real app we'd need a DAO method to get single article.
                                    // For now, let's assume we can reconstruct or fetch.
                                    // Ideally, we need 'getArticleById'.
                                    // Let's implement that in DAO quickly or just fetch all and find (inefficient but works for prototype).
                                    // But wait, I can just re-embed the text if I have it?
                                    // No, I should use the stored text.

                                    // Hack: just finding it from the list for now to demonstrate logic
                                    val articles = database.articleDao().getRelevantArticles().first()
                                    val article = articles.find { it.id == id }

                                    if (article != null) {
                                        // 2. Generate embedding
                                        val text = "${article.title} ${article.description}"
                                        val embedding = scorer.generateEmbedding(text)

                                        if (embedding != null) {
                                            // 3. Save as UserInterest
                                            val embeddingStr = embedding.joinToString(",")
                                            database.userInterestDao().insertInterest(
                                                UserInterest(vectorEmbedding = embeddingStr)
                                            )
                                            call.respondText("Interest recorded", status = HttpStatusCode.OK)
                                        } else {
                                             call.respondText("Could not generate embedding (Model missing?)", status = HttpStatusCode.ServiceUnavailable)
                                        }
                                    } else {
                                        call.respondText("Article not found", status = HttpStatusCode.NotFound)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    call.respondText("Error", status = HttpStatusCode.InternalServerError)
                                }
                            } else {
                                call.respondText("Invalid ID", status = HttpStatusCode.BadRequest)
                            }
                        }
                    }
                }.start(wait = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val channelId = "server_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "RSS Server",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SentinelRSS Server")
            .setContentText("Server is running on port 8080")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
}
