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
import com.sentinelrss.data.local.AppDatabase
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
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
                                call.respondText("Error fetching articles", status = io.ktor.http.HttpStatusCode.InternalServerError)
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
