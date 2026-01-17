package com.sentinelrss.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [Feed::class, Article::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun articleDao(): ArticleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Initialize SQLCipher if needed.
                // With 4.5.4, we use SupportFactory directly.

                val passphrase = "sentinel_secret".toByteArray()
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sentinel_rss.db"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration() // For development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
