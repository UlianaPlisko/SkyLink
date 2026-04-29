package com.codepalace.accelerometer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SpaceObjectEntity::class,
        SpaceObjectDetailEntity::class,
        FavoriteEntity::class,
        PendingFavoriteActionEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun spaceObjectDao(): SpaceObjectDao

    abstract fun spaceObjectDetailDao(): SpaceObjectDetailDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun pendingFavoriteActionDao(): PendingFavoriteActionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE space_object_details ADD COLUMN orbitalModel TEXT")
                db.execSQL("ALTER TABLE space_object_details ADD COLUMN lastComputed TEXT")
                db.execSQL("ALTER TABLE space_object_details ADD COLUMN catalogId TEXT")
                db.execSQL("ALTER TABLE space_object_details ADD COLUMN angularSize REAL")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS favorite_objects (
                        spaceObjectId INTEGER NOT NULL PRIMARY KEY,
                        favoriteId INTEGER NOT NULL,
                        displayName TEXT NOT NULL,
                        magnitude REAL NOT NULL,
                        objectType TEXT NOT NULL,
                        raDeg REAL NOT NULL,
                        decDeg REAL NOT NULL,
                        description TEXT,
                        note TEXT,
                        visibility REAL NOT NULL,
                        addedAt TEXT,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_favorite_actions (
                        spaceObjectId INTEGER NOT NULL PRIMARY KEY,
                        action TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "celestial_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
