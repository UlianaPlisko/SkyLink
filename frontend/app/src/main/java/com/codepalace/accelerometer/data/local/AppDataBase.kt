package com.codepalace.accelerometer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SpaceObjectEntity::class, SpaceObjectDetailEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun spaceObjectDao(): SpaceObjectDao

    abstract fun spaceObjectDetailDao(): SpaceObjectDetailDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "celestial_database"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}