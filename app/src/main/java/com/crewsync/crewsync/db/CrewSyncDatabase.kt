package com.example.myapplication.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class CrewSyncDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: CrewSyncDatabase? = null

        fun getInstance(context: Context): CrewSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CrewSyncDatabase::class.java,
                    "crewsync_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

