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
        PendingFavoriteActionEntity::class,
        EventEntity::class,
        ChatRoomEntity::class,
        PendingChatActionEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun spaceObjectDao(): SpaceObjectDao

    abstract fun spaceObjectDetailDao(): SpaceObjectDetailDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun pendingFavoriteActionDao(): PendingFavoriteActionDao

    abstract fun eventDao(): EventDao

    abstract fun chatRoomDao(): ChatRoomDao

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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                eventType TEXT NOT NULL,
                startAt TEXT NOT NULL,
                endAt TEXT,
                creatorId INTEGER NOT NULL,
                participantsCount INTEGER NOT NULL
            )
            """.trimIndent()
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            ALTER TABLE events ADD COLUMN isParticipant INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_rooms (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        regionGeom TEXT,
                        eventId INTEGER,
                        createdBy INTEGER,
                        createdAt TEXT,
                        isSubscribed INTEGER NOT NULL DEFAULT 0,
                        unreadCount INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add userId column to existing chat_rooms
                db.execSQL("ALTER TABLE chat_rooms ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")

                // Create pending actions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_chat_actions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        roomId INTEGER NOT NULL,
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
                    .addMigrations(
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11
                    )
                    .fallbackToDestructiveMigration(false)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
