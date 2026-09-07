package com.example.taptect.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TapRecord::class], version = 1, exportSchema = false)
abstract class TapDatabase : RoomDatabase() {
    abstract fun tapDao(): TapDao

    companion object {
        @Volatile
        private var INSTANCE: TapDatabase? = null

        fun getDatabase(context: Context): TapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TapDatabase::class.java,
                    "tap_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
