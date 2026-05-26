package com.morsmek.phantm.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        IdentitySettings::class,
        ContactEntity::class,
        MessageEntity::class,
        PendingRequestEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun identityDao(): IdentityDao
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun pendingRequestDao(): PendingRequestDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS identity_settings_new (" +
                    "id INTEGER PRIMARY KEY NOT NULL, " +
                    "mnemonic TEXT, " +
                    "publicKey TEXT, " +
                    "displayName TEXT NOT NULL, " +
                    "isOnboarded INTEGER NOT NULL, " +
                    "appLockEnabled INTEGER NOT NULL, " +
                    "notificationsEnabled INTEGER NOT NULL, " +
                    "showNotificationPreview INTEGER NOT NULL, " +
                    "autoDeleteDays INTEGER NOT NULL" +
                    ")")
                database.execSQL("INSERT INTO identity_settings_new SELECT " +
                    "id, mnemonic, publicKey, displayName, isOnboarded, " +
                    "appLockEnabled, notificationsEnabled, showNotificationPreview, autoDeleteDays " +
                    "FROM identity_settings")
                database.execSQL("DROP TABLE identity_settings")
                database.execSQL("ALTER TABLE identity_settings_new RENAME TO identity_settings")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `pending_requests` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `introMessage` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE identity_settings ADD COLUMN appLockPinHash TEXT")
                database.execSQL("ALTER TABLE identity_settings ADD COLUMN appLockSalt TEXT")
                database.execSQL("ALTER TABLE identity_settings ADD COLUMN appLockFailedAttempts INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE identity_settings ADD COLUMN appLockLastFailedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS identity_settings_new (" +
                    "id INTEGER PRIMARY KEY NOT NULL, " +
                    "publicKey TEXT, " +
                    "displayName TEXT NOT NULL, " +
                    "isOnboarded INTEGER NOT NULL, " +
                    "appLockEnabled INTEGER NOT NULL, " +
                    "notificationsEnabled INTEGER NOT NULL, " +
                    "showNotificationPreview INTEGER NOT NULL, " +
                    "autoDeleteDays INTEGER NOT NULL, " +
                    "appLockPinHash TEXT, " +
                    "appLockSalt TEXT, " +
                    "appLockFailedAttempts INTEGER NOT NULL, " +
                    "appLockLastFailedAt INTEGER NOT NULL" +
                    ")")
                database.execSQL("INSERT INTO identity_settings_new SELECT " +
                    "id, publicKey, displayName, isOnboarded, " +
                    "appLockEnabled, notificationsEnabled, showNotificationPreview, autoDeleteDays, " +
                    "appLockPinHash, appLockSalt, appLockFailedAttempts, appLockLastFailedAt " +
                    "FROM identity_settings")
                database.execSQL("DROP TABLE identity_settings")
                database.execSQL("ALTER TABLE identity_settings_new RENAME TO identity_settings")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Initialize SQLCipher libraries
                SQLiteDatabase.loadLibs(context.applicationContext)
                
                val dbPassphrase = DatabaseKeyManager.getOrGenerateDatabaseKey(context.applicationContext)
                val factory = SupportFactory(dbPassphrase.toByteArray(Charsets.UTF_8))
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phantm_database"
                )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
