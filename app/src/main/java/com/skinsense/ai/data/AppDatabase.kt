package com.skinsense.ai.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class, ChatMessageEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "skinsense_database"
                )
                .fallbackToDestructiveMigration() // Handle migration if needed
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
