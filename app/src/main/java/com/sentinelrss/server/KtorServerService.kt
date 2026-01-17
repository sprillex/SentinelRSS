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
