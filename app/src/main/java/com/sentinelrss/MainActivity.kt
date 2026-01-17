package com.sentinelrss

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sentinelrss.data.local.AppDatabase
import com.sentinelrss.data.local.Feed
import com.sentinelrss.server.KtorServerService
import com.sentinelrss.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the server service
        val intent = Intent(this, KtorServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        val db = AppDatabase.getDatabase(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        db = db,
                        onCloseApp = {
                            stopService(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(db: AppDatabase, onCloseApp: () -> Unit) {
    var showSettings by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    val ipAddress = remember { NetworkUtils.getLocalIpAddress() }

    if (showSettings) {
        SettingsScreen(db = db, onBack = { showSettings = false })
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Sentinel RSS Running...", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "IP Address: $ipAddress", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Dashboard Port: 8080", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { showSettings = true }) {
                Text("Settings / Manage Feeds")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showCloseDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Close App")
            }
        }
    }

    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            title = { Text("Close SentinelRSS?") },
            text = { Text("Are you sure you want to stop the server and exit?") },
            confirmButton = {
                Button(
                    onClick = {
                        showCloseDialog = false
                        onCloseApp()
                    },
                     colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Close")
                }
            },
            dismissButton = {
                Button(onClick = { showCloseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsScreen(db: AppDatabase, onBack: () -> Unit) {
    var feeds by remember { mutableStateOf<List<Feed>>(emptyList()) }
    var newFeedUrl by remember { mutableStateOf("") }
    var newFeedTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            feeds = db.feedDao().getAllFeedsSync()
        }
    }

    fun refreshFeeds() {
        scope.launch(Dispatchers.IO) {
            feeds = db.feedDao().getAllFeedsSync()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Manage Feeds", style = MaterialTheme.typography.headlineSmall)

        // Add Feed Section
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = newFeedTitle,
                    onValueChange = { newFeedTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newFeedUrl,
                    onValueChange = { newFeedUrl = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newFeedUrl.isNotBlank() && newFeedTitle.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                db.feedDao().insertFeed(
                                    Feed(
                                        url = newFeedUrl,
                                        title = newFeedTitle,
                                        category = "User",
                                        downloadLimit = 10
                                    )
                                )
                                newFeedUrl = ""
                                newFeedTitle = ""
                                refreshFeeds()
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                ) {
                    Text("Add Feed")
                }
            }
        }

        // Feed List
        LazyColumn {
            items(feeds) { feed ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = feed.title, style = MaterialTheme.typography.bodyLarge)
                        Text(text = feed.url, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.feedDao().deleteFeed(feed.id)
                            refreshFeeds()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
                Divider()
            }
        }
    }
}
