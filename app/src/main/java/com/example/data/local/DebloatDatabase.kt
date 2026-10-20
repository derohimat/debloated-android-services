package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SystemAppEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class DebloatDatabase : RoomDatabase() {

    abstract fun systemAppDao(): SystemAppDao

    companion object {
        private const val DATABASE_NAME = "debloat_master.db"

        @Volatile
        private var INSTANCE: DebloatDatabase? = null

        fun getInstance(context: Context): DebloatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DebloatDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
