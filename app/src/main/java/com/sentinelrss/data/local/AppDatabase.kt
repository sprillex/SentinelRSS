package com.sentinelrss.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sqlcipher.database.SupportFactory

@Database(entities = [Feed::class, Article::class, UserInterest::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun articleDao(): ArticleDao
    abstract fun userInterestDao(): UserInterestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = "sentinel_secret".toByteArray()
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sentinel_rss.db"
                )
                .openHelperFactory(factory)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed data
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                populateDatabase(database.feedDao())
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateDatabase(feedDao: FeedDao) {
            val feeds = listOf(
                Feed(url = "https://hackaday.com/blog/feed/", title = "Hackaday", category = "Hackaday", downloadLimit = 10),
                Feed(url = "https://feeds.feedburner.com/TroyHunt", title = "Troy Hunt", category = "Troy Hunt", downloadLimit = 10),
                Feed(url = "https://www.jeffgeerling.com/blog.xml", title = "Jeff Geerling", category = "Jeff Geerling", downloadLimit = 10),
                Feed(url = "https://www.blackhillsinfosec.com/blog/feed/", title = "BHIS", category = "BHIS", downloadLimit = 10),
                Feed(url = "https://www.toledoblade.com/rss/", title = "Toledo Blade", category = "Local", downloadLimit = 5),
                Feed(url = "https://feeds.feedblitz.com/wtol/news&x=1", title = "WTOL News", category = "Local", downloadLimit = 5),
                Feed(url = "https://www.404media.co/rss/", title = "404 Media", category = "404", downloadLimit = 5),
                Feed(url = "https://techcrunch.com/feed/", title = "TechCrunch", category = "Tech", downloadLimit = 5),
                Feed(url = "https://www.wired.com/feed/rss", title = "Wired", category = "Tech", downloadLimit = 5),
                Feed(url = "https://www.newscientist.com/feed/home/?cmpid=RSS%7CNSNS-Home", title = "New Scientist", category = "Science", downloadLimit = 5),
                Feed(url = "https://www.engadget.com/rss.xml", title = "Engadget", category = "Tech", downloadLimit = 5),
                Feed(url = "https://feeds.bbci.co.uk/news/world/rss.xml", title = "BBC World News", category = "News", downloadLimit = 5)
            )
            feedDao.insertFeeds(feeds)
        }
    }
}
